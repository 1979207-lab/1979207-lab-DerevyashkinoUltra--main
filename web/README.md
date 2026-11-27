# Деревяшкино 22 — веб-клиент

Статический интерфейс для GitHub Pages. Для подключения к тому же Firebase проекту:
1. В Firebase Console откройте Project settings → General → Your apps.
2. Создайте Web App (если ещё нет) и скопируйте config.
3. Скопируйте firebase-config.example.js как firebase-config.js и вставьте config.
4. В Firebase Authentication включите Email/Password и добавьте домен GitHub Pages в Authorized domains.
5. Опубликуйте папку web на GitHub Pages.

Важно: это начальный этап веб-клиента, а не полная реализация всех экранов Android. Сейчас есть авторизация и чтение списка заказов; CRUD заказов, клиентов, финансы, чат и синхронизация статуса оплаты ещё требуют интеграции. Не включайте публичный доступ к Firestore правилами allow read, write: if true.

Публикация: GitHub Actions → Deploy Derevyashkino 22 Web. Источник GitHub Pages: GitHub Actions.
