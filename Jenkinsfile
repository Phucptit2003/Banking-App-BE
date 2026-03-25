// Jenkinsfile
// Đặt file này ở ROOT của project (cùng cấp pom.xml)
// Jenkins sẽ tự đọc khi có git push

pipeline {
    agent any

    // Biến môi trường dùng trong pipeline
    environment {
        APP_NAME    = 'banking-demo'
        JAR_NAME    = 'banking-demo-1.0.0.jar'
        DEPLOY_DIR  = '/home/ubuntu/app'
        JAVA_HOME   = '/usr/lib/jvm/java-17-openjdk-amd64'

        // Jenkins tự giải mã từ Credentials vault — không bao giờ in ra log
        DB_PASSWORD = credentials('DB_PASSWORD')
    }

    stages {

        // ----------------------------------------
        stage('1. Checkout') {
        // ----------------------------------------
            steps {
                echo "📥 Đang pull code từ Git..."
                checkout scm
            }
        }

        // ----------------------------------------
        stage('2. Build') {
        // ----------------------------------------
            steps {
                echo "🔨 Đang build JAR..."
                sh 'mvn clean package -DskipTests'
                echo "✅ Build xong: target/${JAR_NAME}"
            }
        }

        // ----------------------------------------
        stage('3. Test') {
        // ----------------------------------------
            steps {
                echo "🧪 Đang chạy unit test..."
                sh 'mvn test'
            }
            post {
                always {
                    // Hiện kết quả test trên Jenkins UI
                    junit '**/target/surefire-reports/*.xml'
                }
                failure {
                    echo "❌ Test thất bại! Dừng pipeline."
                }
            }
        }

        // ----------------------------------------
        stage('4. Deploy') {
        // ----------------------------------------
            steps {
                echo "🚀 Đang deploy lên EC2..."

                // Bước 4a: Copy JAR mới vào thư mục deploy
                sh """
                    cp target/${JAR_NAME} ${DEPLOY_DIR}/${JAR_NAME}
                    echo "✅ Copy JAR xong"
                """

                // Bước 4b: Jenkins inject DB_PASSWORD vào systemd service
                // - DB_PASSWORD lấy từ Jenkins Credentials vault (đã mã hóa)
                // - Jenkins tự che password trong log, hiện là ****
                // - Không có file .env, không lưu password trên EC2
                sh '''
                    sudo sed -i "s|Environment=\\"DB_PASSWORD=.*\\"|Environment=\\"DB_PASSWORD=${DB_PASSWORD}\\"|" \
                        /etc/systemd/system/banking.service
                    sudo systemctl daemon-reload
                    echo "✅ Đã inject DB_PASSWORD vào service"
                '''

                // Bước 4c: Restart ứng dụng
                sh """
                    sudo systemctl restart banking
                    sleep 10
                    sudo systemctl status banking --no-pager
                """
            }
        }

        // ----------------------------------------
        stage('5. Health Check') {
        // ----------------------------------------
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

    // Thông báo kết quả sau mỗi lần build
    post {
        success {
            echo """
            ============================================
            ✅ DEPLOY THÀNH CÔNG!
            App: http://<EC2_IP>:8080
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
