plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.yirankuma.yritems"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(18))
    }
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}



dependencies {
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

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
    archiveVersion.set("")
    archiveClassifier.set("")  // 添加这行来移除-all后缀
    destinationDirectory.set(file("E:/ServerPLUGINS/网易NK服务器插件"))

    doFirst {
        destinationDirectory.get().asFile.mkdirs()
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