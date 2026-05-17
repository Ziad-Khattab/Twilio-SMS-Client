<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Verify Phone - Twilio SMS</title>
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

        .form-actions {
            display: flex;
            flex-direction: column;
            gap: 0.75rem;
            margin-top: 1rem;
        }

        .link-btn {
            background: none;
            border: none;
            color: var(--primary-color);
            cursor: pointer;
            font-size: 0.9rem;
            padding: 0;
        }

        .link-btn:hover {
            text-decoration: underline;
        }
    </style>
</head>

<body>
    <div class="auth-container">
        <div class="auth-card">
            <div class="auth-header">
                <h1>Verify your phone</h1>
                <p>Enter the 6-digit code sent to
                    <strong><%= request.getAttribute("msisdn") != null ? request.getAttribute("msisdn") : "your number" %></strong>
                </p>
            </div>

            <% if (request.getAttribute("message") != null) { %>
                <div class="message success"><%= request.getAttribute("message") %></div>
            <% } %>
            <% if (request.getAttribute("error") != null) { %>
                <div class="message error"><%= request.getAttribute("error") %></div>
            <% } %>

            <form action="verify-msisdn" method="post" class="auth-form">
                <div class="form-group">
                    <label for="code">Verification code</label>
                    <input type="text" id="code" name="code" placeholder="123456"
                        pattern="[0-9]{6}" maxlength="6" inputmode="numeric" required autofocus>
                </div>
                <button type="submit">Complete registration</button>
            </form>

            <div class="form-actions">
                <form action="verify-msisdn" method="post">
                    <input type="hidden" name="action" value="resend">
                    <button type="submit" class="link-btn">Resend code</button>
                </form>
                <form action="verify-msisdn" method="post">
                    <input type="hidden" name="action" value="cancel">
                    <button type="submit" class="link-btn">Cancel and start over</button>
                </form>
            </div>
        </div>
    </div>
</body>

</html>
