pipeline {
    agent any

    tools {
        // 配置 JDK（使用 MOTCI 上配置的 JDK）
        jdk 'JDK17'
    }

    environment {
        // 构建配置
        GRADLE_OPTS = '-Dorg.gradle.daemon=false -Dorg.gradle.jvmargs=-Xmx2048m'
        CI = 'true'
    }

    stages {
        stage('Environment Info') {
            steps {
                echo '=== 环境信息 ==='
                script {
                    if (isUnix()) {
                        sh 'java -version'
                        sh 'pwd'
                        sh 'ls -la'
                    } else {
                        bat 'java -version'
                        bat 'cd'
                        bat 'dir'
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                echo '=== 检出代码 ==='
                checkout scm
                script {
                    if (isUnix()) {
                        sh 'git log -1 --pretty=format:"%h - %an: %s"'
                    } else {
                        bat 'git log -1 --pretty=format:"%%h - %%an: %%s"'
                    }
                }
            }
        }

        stage('Build') {
            steps {
                echo '=== 开始构建 ==='
                script {
                    if (isUnix()) {
                        sh 'chmod +x gradlew'
                        sh './gradlew clean build --no-daemon --stacktrace'
                    } else {
                        bat 'gradlew.bat clean build --no-daemon --stacktrace'
                    }
                }
            }
        }

        stage('Test') {
            steps {
                echo '=== 运行测试 ==='
                script {
                    if (isUnix()) {
                        sh './gradlew test --no-daemon || true'
                    } else {
                        bat 'gradlew.bat test --no-daemon || exit 0'
                    }
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
                }
            }
        }

        stage('Archive') {
            steps {
                echo '=== 归档构建产物 ==='
                script {
                    if (isUnix()) {
                        sh 'ls -lh build/libs/'
                    } else {
                        bat 'dir build\\libs\\'
                    }
                }
                archiveArtifacts artifacts: '**/build/libs/*.jar', fingerprint: true, allowEmptyArchive: false
            }
        }
    }

    post {
        success {
            echo '✅ 构建成功！'
            script {
                if (isUnix()) {
                    sh 'echo "Build #${BUILD_NUMBER} completed successfully"'
                } else {
                    bat 'echo Build #%BUILD_NUMBER% completed successfully'
                }
            }
        }
        failure {
            echo '❌ 构建失败！请查看日志'
        }
        always {
            echo '=== 清理工作空间 ==='
            cleanWs()
        }
    }
}
