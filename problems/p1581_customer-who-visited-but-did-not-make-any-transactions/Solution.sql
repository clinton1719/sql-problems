SELECT
    customer_id,
    COUNT(visit_id) as count_no_trans
FROM
    Visits
WHERE
    visit_id NOT IN (
        SELECT
            visit_id
        FROM
            Transactions
    )
GROUP BY
    customer_id;

-- OR
SELECT
    customer_id,
    COUNT(*) as count_no_trans
FROM
    Visits as v
    LEFT JOIN Transactions as t ON v.visit_id = t.visit_id
WHERE
    t.visit_id is NULL
GROUP BY
    customer_id;