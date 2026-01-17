plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.yirankuma.yritems"
version = "1.0-SNAPSHOT"

// 智能检测 Java Home：CI 环境使用 Jenkins 配置的 JDK，本地使用 gradle.properties 配置
if (System.getenv("CI") != "true" && project.hasProperty("org.gradle.java.home")) {
    val javaHomePath = project.property("org.gradle.java.home") as String
    if (file(javaHomePath).exists()) {
        println("使用本地 JDK: $javaHomePath")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
    maven("https://repo.opencollab.dev/maven-releases/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
}

dependencies {
    // Nukkit MOT 依赖（从 Maven 仓库获取）
    compileOnly("cn.nukkit:nukkit:1.0-SNAPSHOT")

    // 添加 Rhino JavaScript 引擎
    implementation("org.mozilla:rhino:1.7.14")

    // 添加 ASM 字节码操作库
    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-commons:9.6")
    implementation("org.ow2.asm:asm-util:9.6")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

// 配置Shadow JAR任务
tasks.shadowJar {
    archiveBaseName.set("YRItems")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")

    // 仅在本地开发时使用自定义输出目录
    if (System.getenv("CI") == null) {
        val localOutputDir = file("E:/ServerPLUGINS/网易NK服务器插件")
        if (localOutputDir.exists()) {
            destinationDirectory.set(localOutputDir)
        }
    }
}

// 禁用默认的jar任务，使用shadowJar
tasks.jar {
    enabled = false
}

// 让build任务依赖shadowJar
tasks.build {
    dependsOn(tasks.shadowJar)
}