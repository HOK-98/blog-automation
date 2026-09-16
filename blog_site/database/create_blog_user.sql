-- Run this as a MariaDB administrator (for example, root).
-- Replace CHANGE_THIS_STRONG_PASSWORD before execution.

CREATE USER IF NOT EXISTS 'blog_user'@'localhost'
    IDENTIFIED BY 'CHANGE_THIS_STRONG_PASSWORD';

-- Also sets the intended password if the account already exists.
ALTER USER 'blog_user'@'localhost'
    IDENTIFIED BY 'CHANGE_THIS_STRONG_PASSWORD';

-- The web application only performs CRUD operations on its own database.
GRANT SELECT, INSERT, UPDATE, DELETE
    ON `blog_site`.*
    TO 'blog_user'@'localhost';

SHOW GRANTS FOR 'blog_user'@'localhost';
