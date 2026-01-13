pipeline {
    agent any
    
tools {
        maven 'Maven-3.9.11'
    }
    
    environment {
        // 从 Jenkins Credentials 中读取
        DOCKER_USERNAME = credentials('docker-username')
        SERVER_HOST = credentials('server-host')
        APPLICATION_PROD_ENV = credentials('application-prod-env')
    }
    
    stages {
        stage('1. 拉取代码') {
            steps {
                checkout scm
            }
        }
        
        stage('2. 单元测试') {
            steps {
                echo '正在运行测试...'
                script {
                    withCredentials([
                        // 获取生产环境配置文件
                        file(credentialsId: 'application-prod-env', variable: 'APP_ENV_FILE')
                    ]) {
                        // 将环境变量文件复制到工作目录，供测试使用
                        sh '''
                            cp ${APP_ENV_FILE} application-prod.env
                            echo "已加载生产环境配置文件"
                        '''
                        // 运行测试，可以通过环境变量或配置文件使用
                        sh 'mvn test'
                    }
                }
            }
        }
        
        stage('3. Maven 打包') {
            steps {
                echo '构建 Jar 包...'
                sh 'mvn clean package -DskipTests'
            }
        }
        
        stage('4. 构建并推送 Docker 镜像') {
            when {
                // 只有非 PR 请求时才构建和推送镜像
                not { changeRequest() }
            }
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        sh '''
                            echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
                            docker build -t ${DOCKER_USER}/firmament-server:latest -f firmament-server/Dockerfile ./firmament-server
                            docker push ${DOCKER_USER}/firmament-server:latest
                        '''
                    }
                }
            }
        }
        
        stage('5. 部署到服务器') {
            when {
                // 只有同时满足：是 main 分支 且 不是 PR 请求
                allOf {
                    branch 'main'
                    not { changeRequest() }
                }
            }
            steps {
                echo '🚀 生产环境部署启动...'
                script {
                    withCredentials([
                        sshUserPrivateKey(
                            credentialsId: 'server-ssh-key',
                            keyFileVariable: 'SSH_KEY',
                            usernameVariable: 'SSH_USER'
                        ),
                        string(credentialsId: 'server-host', variable: 'SERVER_HOST'),
                        string(credentialsId: 'docker-username', variable: 'DOCKER_USERNAME'),
                        
                        // ⚠️ 关键修改 1：这里改成了 file 类型
                        // 这里的 APP_ENV_FILE 变量代表的是 Jenkins 临时生成的一个文件路径
                        file(credentialsId: 'application-prod-env', variable: 'APP_ENV_FILE')
                    ]) {
                        
                        // ⚠️ 关键修改 2：不再使用 writeFile，而是直接把秘密文件复制出来重命名
                        // 这样就完美保留了文件里的换行符
                        sh "cp ${APP_ENV_FILE} app_env.tmp"
                        
                        // 生成部署脚本
                        def deployScript = """#!/bin/bash
                        set -e
                        
                        mkdir -p /opt/firmament/config
                        
                        mv /tmp/application-prod.env.tmp /opt/firmament/config/application-prod.env
                        chmod 600 /opt/firmament/config/application-prod.env
                        
                        echo "正在拉取镜像..."
                        docker pull ${DOCKER_USERNAME}/firmament-server:latest
                        
                        echo "清理旧容器..."
                        docker stop firmament-server || true
                        docker rm firmament-server || true
                        
                        echo "启动新容器..."
                        docker run -d \\
                            --name firmament-server \\
                            --network firmament_app-network \\
                            --env-file /opt/firmament/config/application-prod.env \\
                            ${DOCKER_USERNAME}/firmament-server:latest
                        """
                        
                        writeFile file: 'deploy.sh', text: deployScript
                        
                        // 执行传输和运行
                        sh """
                            mkdir -p ~/.ssh
                            cp "${SSH_KEY}" ~/.ssh/deploy_key
                            chmod 600 ~/.ssh/deploy_key
                            
                            echo "正在上传文件到远程服务器..."
                            scp -i ~/.ssh/deploy_key -o StrictHostKeyChecking=no app_env.tmp ${SSH_USER}@${SERVER_HOST}:/tmp/application-prod.env.tmp
                            scp -i ~/.ssh/deploy_key -o StrictHostKeyChecking=no deploy.sh ${SSH_USER}@${SERVER_HOST}:/tmp/deploy.sh
                            
                            echo "正在执行远程部署..."
                            ssh -i ~/.ssh/deploy_key -o StrictHostKeyChecking=no ${SSH_USER}@${SERVER_HOST} "chmod +x /tmp/deploy.sh && bash /tmp/deploy.sh"
                            
                            rm -f ~/.ssh/deploy_key app_env.tmp deploy.sh
                        """
                    }
                }
            }
        }
    }
 
    post {
        always {
            cleanWs() // 清理工作空间
        }
    }
}