<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <!DOCTYPE html>
    <html lang="en">

    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Twilio SMS - Send Messages Easily</title>
        <link rel="stylesheet" href="css/style.css">
        <style>
            body {
                padding: 0;
            }

            .navbar {
                background: var(--white);
                padding: 1rem 2rem;
                box-shadow: var(--shadow);
                display: flex;
                justify-content: space-between;
                align-items: center;
            }

            .navbar h1 {
                margin: 0;
                font-size: 1.5rem;
            }

            .nav-links {
                display: flex;
                gap: 1rem;
            }

            .nav-links a {
                padding: 0.5rem 1rem;
                border-radius: 4px;
                transition: background-color 0.3s ease;
            }

            .nav-links a.btn-primary {
                background-color: var(--primary-color);
                color: var(--white);
            }

            .nav-links a.btn-primary:hover {
                background-color: var(--primary-dark);
                text-decoration: none;
            }

            .nav-links a.btn-secondary {
                background-color: var(--secondary-color);
                color: var(--white);
            }

            .nav-links a.btn-secondary:hover {
                background-color: var(--secondary-dark);
                text-decoration: none;
            }

            .hero {
                background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-dark) 100%);
                color: var(--white);
                padding: 4rem 2rem;
                text-align: center;
            }

            .hero h2 {
                font-size: 2.5rem;
                margin-bottom: 1rem;
                color: var(--white);
            }

            .hero p {
                font-size: 1.25rem;
                margin-bottom: 2rem;
                opacity: 0.95;
            }

            .hero .btn {
                background-color: var(--white);
                color: var(--primary-color);
                font-weight: 600;
                padding: 1rem 2rem;
                font-size: 1.1rem;
            }

            .hero .btn:hover {
                background-color: var(--light-bg);
            }

            .features {
                padding: 4rem 2rem;
                max-width: 1200px;
                margin: 0 auto;
            }

            .features h2 {
                text-align: center;
                font-size: 2rem;
                margin-bottom: 3rem;
            }

            .feature-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
                gap: 2rem;
            }

            .feature-card {
                background: var(--white);
                padding: 2rem;
                border-radius: var(--border-radius);
                box-shadow: var(--shadow);
                text-align: center;
                transition: transform 0.3s ease, box-shadow 0.3s ease;
            }

            .feature-card:hover {
                transform: translateY(-5px);
                box-shadow: 0 5px 20px rgba(0, 0, 0, 0.15);
            }

            .feature-icon {
                font-size: 3rem;
                margin-bottom: 1rem;
            }

            .feature-card h3 {
                margin-bottom: 1rem;
            }

            .feature-card p {
                color: var(--text-muted);
            }

            .footer {
                background: var(--text-dark);
                color: var(--white);
                text-align: center;
                padding: 2rem;
                margin-top: 4rem;
            }

            @media (max-width: 768px) {
                .navbar {
                    flex-direction: column;
                    gap: 1rem;
                }

                .hero h2 {
                    font-size: 1.75rem;
                }

                .hero p {
                    font-size: 1rem;
                }
            }
        </style>
    </head>

    <body>
        <nav class="navbar">
            <h1>📱 Twilio SMS</h1>
            <div class="nav-links">
                <a href="login" class="btn-secondary">Login</a>
                <a href="register" class="btn-primary">Register</a>
            </div>
        </nav>

        <section class="hero">
            <h2>Send SMS Messages with Ease</h2>
            <p>A simple and powerful platform to send SMS messages to your contacts</p>
            <a href="register" class="btn">Get Started</a>
        </section>

        <section class="features">
            <h2>Why Choose Twilio SMS?</h2>
            <div class="feature-grid">
                <div class="feature-card">
                    <div class="feature-icon">✉️</div>
                    <h3>Easy to Use</h3>
                    <p>Send SMS messages with just a few clicks. No complicated setup required.</p>
                </div>
                <div class="feature-card">
                    <div class="feature-icon">📊</div>
                    <h3>Track History</h3>
                    <p>Keep track of all your sent messages with detailed history and status updates.</p>
                </div>
                <div class="feature-card">
                    <div class="feature-icon">🔒</div>
                    <h3>Secure</h3>
                    <p>Your data is protected with secure authentication and encryption.</p>
                </div>
                <div class="feature-card">
                    <div class="feature-icon">⚡</div>
                    <h3>Fast Delivery</h3>
                    <p>Messages are delivered instantly to recipients worldwide.</p>
                </div>
                <div class="feature-card">
                    <div class="feature-icon">📱</div>
                    <h3>Mobile Friendly</h3>
                    <p>Access your dashboard from any device, anytime, anywhere.</p>
                </div>
                <div class="feature-card">
                    <div class="feature-icon">💬</div>
                    <h3>Reliable</h3>
                    <p>Built on Twilio's trusted infrastructure for reliable message delivery.</p>
                </div>
            </div>
        </section>

        <footer class="footer">
            <p>&copy; 2026 Twilio SMS. All rights reserved.</p>
        </footer>
    </body>

    </html>