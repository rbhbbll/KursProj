-- Миграция для добавления поля user_id в таблицу clients
-- Это свяжет пользователей (users) с клиентами (clients)

-- Добавляем поле user_id в таблицу clients
ALTER TABLE public.clients ADD COLUMN user_id INTEGER;

-- Добавляем внешний ключ на таблицу users
ALTER TABLE public.clients 
ADD CONSTRAINT clients_user_id_fkey 
FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

-- Добавляем индекс для быстрого поиска
CREATE INDEX idx_clients_user_id ON public.clients(user_id);

-- Обновляем существующие записи клиентов (если есть)
-- ВАЖНО: Это нужно выполнить вручную для существующих данных
-- Пример для обновления существующих клиентов:
-- UPDATE public.clients SET user_id = (SELECT id FROM public.users WHERE username = 'имя_пользователя') WHERE id = клиент_id;

-- Комментарий: После выполнения этой миграции все новые клиенты должны регистрироваться 
-- через новый метод registerClientWithUser, который автоматически создает связь между user и client
