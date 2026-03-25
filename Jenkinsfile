pipeline {
    agent any

    tools {
        maven 'Maven 3'
    }

    environment {
        JAR_NAME    = 'banking-demo-1.0.0.jar'
        DEPLOY_DIR  = '/home/ubuntu/app'
        JAVA_HOME   = '/opt/java/openjdk'
        DB_PASSWORD = credentials('DB_PASSWORD')
    }

    stages {

        stage('1. Checkout') {
            steps {
                echo "📥 Đang pull code từ Git..."
                checkout scm
            }
        }

        stage('2. Build') {
            steps {
                echo "🔨 Đang build JAR..."
                sh 'mvn clean package -DskipTests'
                echo "✅ Build xong!"
            }
        }

        stage('3. Test') {
            steps {
                echo "⏭️ Bỏ qua test trên CI"
                sh 'echo "Skipped"'
            }
        }

        stage('4. Deploy') {
            steps {
                echo "🚀 Đang deploy..."

                // Bước 1: Copy JAR
                sh """
                    cp \${WORKSPACE}/target/${JAR_NAME} ${DEPLOY_DIR}/${JAR_NAME}
                    echo "✅ Copy JAR xong"
                """

                // Bước 2: Ghi lại toàn bộ service file với password mới
                sh """
                    cat > /etc/systemd/system/banking.service << EOF
        [Unit]
        Description=Banking Spring Boot Application
        After=network.target mysql.service
        Requires=mysql.service

        [Service]
        User=ubuntu
        WorkingDirectory=/home/ubuntu/app
        Environment="DB_PASSWORD=${DB_PASSWORD}"
        Environment="SPRING_PROFILES_ACTIVE=prod"
        ExecStart=/usr/bin/java -jar /home/ubuntu/app/banking-demo-1.0.0.jar
        ExecStop=/bin/kill -SIGTERM \\\$MAINPID
        SuccessExitStatus=143
        StandardOutput=append:/home/ubuntu/app/logs/app.log
        StandardError=append:/home/ubuntu/app/logs/app-error.log
        Restart=on-failure
        RestartSec=15

        [Install]
        WantedBy=multi-user.target
        EOF
                    systemctl daemon-reload
                    systemctl restart banking
                    sleep 10
                    systemctl status banking --no-pager
                    echo "✅ Deploy xong!"
                """
            }
        }

        stage('5. Health Check') {
            steps {
                echo "💓 Kiểm tra app còn sống không..."
                retry(5) {
                    sleep(time: 5, unit: 'SECONDS')
                    sh 'curl -f http://localhost:8080/api/accounts/ACC001 || exit 1'
                }
                echo "✅ App đang chạy bình thường!"
            }
        }
    }

    post {
        success {
            echo """
            ============================================
            ✅ DEPLOY THÀNH CÔNG!
            App: http://3.106.215.148:8080
            Build: #${BUILD_NUMBER}
            ============================================
            """
        }
        failure {
            echo """
            ============================================
            ❌ DEPLOY THẤT BẠI - Build #${BUILD_NUMBER}
            Xem log: ${BUILD_URL}console
            ============================================
            """
        }
    }
}
