# Деревяшкино Ultra — мобильное SPA

## Реализовано
- Адаптивный интерфейс для Android и ПК.
- Firebase Authentication: вход Email/пароль, самостоятельная регистрация отключена.
- Роли owner, admin, employee.
- Заказы: просмотр, поиск, фильтр статуса, создание для владельца и администратора.
- Список клиентов формируется из заказов.
- Реальное обновление заказов через Firestore onSnapshot.
- PWA manifest и базовый service worker.

## Настройка Firebase
1. Создайте проект в Firebase Console.
2. В Authentication → Sign-in method включите Email/Password.
3. В Project settings → Your apps создайте Web app и скопируйте конфигурацию.
4. Скопируйте firebase-config.example.js в firebase-config.js и заполните параметры проекта.
5. Создайте Firestore Database.
6. Опубликуйте firestore.rules в Firestore → Rules.
7. Создайте первого пользователя в Authentication → Users.
8. Скопируйте его UID и создайте документ users/{UID} в Firestore с полями email, name и role: owner.
9. Для сотрудников создавайте Auth-пользователей и документы users/{UID} с ролью admin или employee.

## Запуск
Размещайте приложение на HTTPS-хостинге (Firebase Hosting или GitHub Pages). Для локальной проверки используйте HTTP-сервер, не открывайте index.html через file://.

## Безопасность и статус
- Заполненный firebase-config.js исключён из Git.
- Реальная защита обеспечивается Firebase Auth и Firestore Security Rules.
- Перед использованием проверьте правила на тестовых аккаунтах всех ролей.
- Финансы, расписание, Telegram, чат и управление пользователями — следующие этапы.