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

                sh """
                    cp \${WORKSPACE}/target/${JAR_NAME} ${DEPLOY_DIR}/${JAR_NAME}
                    echo "✅ Copy JAR xong"
                """

                sh """
                    sed -i 's|Environment=\"DB_PASSWORD=.*\"|Environment=\"DB_PASSWORD=${DB_PASSWORD}\"|' \
                        /etc/systemd/system/banking.service
                    echo "✅ Inject DB_PASSWORD xong"
                """

                // Restart app: kill process cũ rồi chạy lại
                sh """
                    # Tắt process cũ nếu đang chạy
                    pkill -f '${JAR_NAME}' || true
                    sleep 5

                    # Chạy app mới ở background
                    nohup java -jar ${DEPLOY_DIR}/${JAR_NAME} \
                        --spring.profiles.active=prod \
                        > ${DEPLOY_DIR}/logs/app.log 2>&1 &

                    echo "✅ App đã restart!"
                """
            }
        }

        stage('5. Health Check') {
            steps {
                echo "💓 Kiểm tra app còn sống không..."
                sleep(time: 20, unit: 'SECONDS')
                sh 'pgrep -f ${JAR_NAME} || exit 1'
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
