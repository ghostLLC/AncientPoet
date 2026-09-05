-- Shared, atomic UTC-day attempt budgets. Provider failures still count toward the cost cap.
CREATE TABLE sms_daily_budgets (
    budget_key VARCHAR(80) NOT NULL,
    budget_day DATE NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    PRIMARY KEY (budget_key,budget_day)
);
