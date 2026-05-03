<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <!DOCTYPE html>
    <html lang="en">

    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Login - Twilio SMS</title>
        <link rel="stylesheet" href="css/style.css">
        <style>
            body {
                display: flex;
                justify-content: center;
                align-items: center;
                min-height: 100vh;
                padding: 1rem;
            }

            .auth-container {
                width: 100%;
                max-width: 400px;
            }

            .auth-card {
                background: var(--white);
                padding: 2rem;
                border-radius: var(--border-radius);
                box-shadow: var(--shadow);
            }

            .auth-header {
                text-align: center;
                margin-bottom: 2rem;
            }

            .auth-header h1 {
                font-size: 1.75rem;
                margin-bottom: 0.5rem;
            }

            .auth-header p {
                color: var(--text-muted);
            }

            .auth-form button {
                width: 100%;
            }

            .auth-footer {
                text-align: center;
                margin-top: 1.5rem;
                color: var(--text-muted);
            }

            .auth-footer a {
                font-weight: 500;
            }
        </style>
    </head>

    <body>
        <div class="auth-container">
            <div class="auth-card">
                <div class="auth-header">
                    <h1>Login</h1>
                    <p>Access your Twilio SMS dashboard</p>
                </div>

                <% if (request.getAttribute("error") !=null) { %>
                    <div class="message error">
                        <%= request.getAttribute("error") %>
                    </div>
                    <% } %>

                        <form action="login" method="post" class="auth-form">
                            <div class="form-group">
                                <label for="username">Username</label>
                                <input type="text" id="username" name="username" placeholder="Enter your username"
                                    required autofocus>
                            </div>
                            <div class="form-group">
                                <label for="password">Password</label>
                                <input type="password" id="password" name="password" placeholder="Enter your password"
                                    required>
                            </div>
                            <button type="submit">Login</button>
                        </form>

                        <div class="auth-footer">
                            <p>Don't have an account? <a href="register.jsp">Register here</a></p>
                        </div>
            </div>
        </div>
    </body>

    </html>