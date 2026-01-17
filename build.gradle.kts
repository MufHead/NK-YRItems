plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.yirankuma.yritems"
version = "1.0-SNAPSHOT"

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
    // 设置JAR文件名（本地和CI都使用）
    archiveBaseName.set("YRItems")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")

    // 只在本地构建时输出到自定义目录，CI环境使用默认目录
    // 通过环境变量判断是否在CI环境（Jenkins）
    val isCI = System.getenv("JENKINS_HOME") != null
    if (!isCI) {
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