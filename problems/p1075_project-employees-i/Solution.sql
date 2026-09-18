SELECT project_id,
       ROUND(AVG(experience_years), 2) AS average_years
FROM Project o
JOIN Employee e ON p.employee_id = e.employee_id
GROUP BY project_id