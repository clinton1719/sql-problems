SELECT
    query_name,
    ROUND(AVG(rating * 1.0 / position), 2) AS quality,
    ROUND((100.0 * SUM(CASE WHEN rating < 3 THEN 1 ELSE 0 END)) / COUNT(rating), 2) AS poor_query_percentage
FROM Queries
GROUP BY query_name;


-- COUNT doesnt count null values. If you do COUNT(rating < 3), it will count all rows, including those with null ratings, which is not what we want. Instead, we use a CASE statement to count only the rows where the rating is less than 3 using SUM. If you want to use COUNT, you need to write COUNT(CASE WHEN rating < 3 THEN 1 ELSE NULL END) to count only the rows where the rating is less than 3.