-- Скрипт для исправления связи между существующими пользователями и клиентами
-- ВАЖНО: Выполните этот скрипт в вашей базе данных kirill123

-- Сначала посмотрим на существующих пользователей и клиентов
SELECT 'Пользователи:' as info;
SELECT id, username, role FROM public.users ORDER BY id;

SELECT 'Клиенты:' as info;
SELECT id, full_name, user_id FROM public.clients ORDER BY id;

-- Создадим связь между пользователями и клиентами по username
-- Это временное решение для существующих данных

-- Обновляем клиентов, связывая их с пользователями по имени
-- ВАЖНО: Замените 'имя_пользователя' на реальные имена пользователей из вашей базы

-- Примеры обновлений (замените на ваши реальные данные):
-- UPDATE public.clients SET user_id = (SELECT id FROM public.users WHERE username = 'admin') WHERE id = 1;
-- UPDATE public.clients SET user_id = (SELECT id FROM public.users WHERE username = '123') WHERE id = 3;
-- UPDATE public.clients SET user_id = (SELECT id FROM public.users WHERE username = 'кирилл') WHERE id = 4;
-- UPDATE public.clients SET user_id = (SELECT id FROM public.users WHERE username = 'rbhbkk') WHERE id = 5;
-- UPDATE public.clients SET user_id = (SELECT id FROM public.users WHERE username = 'мария') WHERE id = 7;
-- UPDATE public.clients SET user_id = (SELECT id FROM public.users WHERE username = 'client2') WHERE id = 8;
-- UPDATE public.clients SET user_id = (SELECT id FROM public.users WHERE username = 'client3') WHERE id = 9;
-- UPDATE public.clients SET user_id = (SELECT id FROM public.users WHERE username = 'client221') WHERE id = 10;
-- UPDATE public.clients SET user_id = (SELECT id FROM public.users WHERE username = 'яяя') WHERE id = 11;
-- UPDATE public.clients SET user_id = (SELECT id FROM public.users WHERE username = '2838') WHERE id = 12;
-- UPDATE public.clients SET user_id = (SELECT id FROM public.users WHERE username = 'овао') WHERE id = 13;
-- UPDATE public.clients SET user_id = (SELECT id FROM public.users WHERE username = 'оамм') WHERE id = 14;
-- UPDATE public.clients SET user_id = (SELECT id FROM public.users WHERE username = 'client4') WHERE id = 15;
-- UPDATE public.clients SET user_id = (SELECT id FROM public.users WHERE username = 'makar') WHERE id = 16;
-- UPDATE public.clients SET user_id = (SELECT id FROM public.users WHERE username = 'makarr') WHERE id = 17;
-- UPDATE public.clients SET user_id = (SELECT id FROM public.users WHERE username = 'kirill') WHERE id = 18;
-- UPDATE public.clients SET user_id = (SELECT id FROM public.users WHERE username = 'маам') WHERE id = 19;
-- UPDATE public.clients SET user_id = (SELECT id FROM public.users WHERE username = 'sultan001') WHERE id = 20;
-- UPDATE public.clients SET user_id = (SELECT id FROM public.users WHERE username = 'qwer') WHERE id = 21;

-- Проверяем результат
SELECT 'Результат после обновления:' as info;
SELECT c.id, c.full_name, c.user_id, u.username, u.role 
FROM public.clients c 
LEFT JOIN public.users u ON c.user_id = u.id 
ORDER BY c.id;

-- Если есть клиенты без user_id, создадим для них пользователей
-- (раскомментируйте если нужно)
/*
INSERT INTO public.users (username, password_hash, role) 
SELECT 
    'client_' || c.id as username,
    '$2a$10$defaultpasswordhash' as password_hash,
    'client' as role
FROM public.clients c 
WHERE c.user_id IS NULL;

UPDATE public.clients 
SET user_id = (SELECT id FROM public.users WHERE username = 'client_' || public.clients.id)
WHERE user_id IS NULL;
*/
