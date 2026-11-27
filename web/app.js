const $=id=>document.getElementById(id);
let auth=null,db=null,user=null,userRole="Сотрудник",currentPage="home",ordersCache=[],transactionsCache=[];
const canManage=()=>userRole==="Владелец"||userRole==="Админ";
const esc=s=>String(s??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
const money=n=>(Number(n)||0).toLocaleString("ru-RU",{minimumFractionDigits:2,maximumFractionDigits:2})+" ₽";
if(window.firebaseConfig&&window.firebaseConfig.apiKey&&window.firebaseConfig.apiKey!=="PASTE_WEB_API_KEY"){
 firebase.initializeApp(window.firebaseConfig);auth=firebase.auth();db=firebase.firestore();
 auth.onAuthStateChanged(async u=>{
 user=u;userRole="Сотрудник";
 if(u){try{const profile=await db.collection("users").doc(u.uid).get();userRole=profile.exists?(profile.get("role")||"Сотрудник"):"Сотрудник";}catch(e){$("status").textContent="Не удалось загрузить профиль: "+e.message;}}
 $("logout").hidden=!u;$("login").hidden=!!u;$("email").hidden=!!u;$("password").hidden=!!u;
 const emailInput=$("email");if(!u&&emailInput&&!emailInput.value){try{emailInput.value=localStorage.getItem("d22-email")||""}catch(_){}}
 document.querySelector('[data-page="finance"]').hidden=!!u&&!canManage();
 $("status").textContent=u?"Вы вошли как "+u.email+" · "+userRole+".":"Войдите в аккаунт.";
 renderPage();
});
}
$("login").onclick=()=>{if(!auth){$("status").textContent="Сначала настройте web/firebase-config.js по примеру.";return}const entered=$("email").value.trim();auth.signInWithEmailAndPassword(entered,$("password").value).then(()=>{try{localStorage.setItem("d22-email",entered)}catch(_){}}).catch(e=>$("status").textContent=e.message)};
$("logout").onclick=()=>auth&&auth.signOut();
function page(name){if(name==="finance"&&!canManage()){currentPage="home";renderPage();return}currentPage=name;document.querySelectorAll("nav button").forEach(b=>b.classList.toggle("active",b.dataset.page===name));renderPage()}
function shell(title,subtitle=""){return '<div class="hero"><p class="eyebrow">ДЕРЕВЯШКИНО 22</p><h1>'+esc(title)+'</h1><p>'+esc(subtitle)+'</p></div>'}
function renderPage(){
 const titles={home:"Главная",orders:"Заказы",clients:"Клиенты",finance:"Финансы",chat:"Чат",archive:"Архив",settings:"Настройки"};
 if(!user){$("content").innerHTML=shell("Деревяшкино 22","Войдите, чтобы работать с заказами и данными мастерской.");return}
 if(currentPage==="orders")return renderOrders();
 if(currentPage==="finance")return canManage()?renderFinance():page("home");
 if(currentPage==="clients")return renderClients();
 if(currentPage==="archive")return renderArchive();
 if(currentPage==="chat")return renderChat();
 if(currentPage==="settings")return renderSettings();
 if(currentPage==="home"){$("content").innerHTML=shell("Деревяшкино 22","Рабочий стол мастерской")+'<div class="tiles"><button data-go="orders">Заказы →</button><button data-go="clients">Клиенты →</button>'+(canManage()?'<button data-go="finance">Финансы →</button>':'')+'<button data-go="chat">Чат →</button><button data-go="archive">Архив →</button></div>';return}
 $("content").innerHTML=shell(titles[currentPage],"");
}
async function renderOrders(){
 $("content").innerHTML=shell("Заказы","Создание, поиск, статусы и отметка оплаты.")+
 (canManage()?'<div class="notice"><h3>Новый заказ</h3><form id="new-order"><input id="order-client" placeholder="Клиент" required><input id="order-service" placeholder="Услуга / изделие" required><input id="order-amount" type="number" min="0.01" step="0.01" placeholder="Сумма, ₽" required><label for="order-payment-new">Оплата</label><select id="order-payment-new"><option value="не оплачено">Не оплачено</option><option value="оплачено">Оплачено</option></select><button>Добавить заказ</button></form></div>'+
 '<div class="notice"><input id="order-search" placeholder="Поиск по клиенту или изделию"><select id="order-status"><option>Все статусы</option><option>новый</option><option>в работе</option><option>выполнен</option><option>отменён</option></select><select id="order-payment"><option>Любая оплата</option><option>Оплачено</option><option>Не оплачено</option></select><div id="order-list">Загрузка…</div></div>';
 try{const snap=await db.collection("orders").get();ordersCache=snap.docs.map(d=>({id:d.id,...d.data()}));drawOrders()}catch(e){$("order-list").textContent=e.message}
 if($("new-order"))$("new-order").onsubmit=async e=>{e.preventDefault();try{await db.collection("orders").add({clientName:$("order-client").value.trim(),service:$("order-service").value.trim(),amount:Number($("order-amount").value),status:"новый",payment:$("order-payment-new").value,createdAt:firebase.firestore.FieldValue.serverTimestamp()});renderOrders()}catch(err){$("order-list").textContent=err.message}};
 ["order-search","order-status","order-payment"].forEach(id=>$(id).addEventListener("input",drawOrders));
}
function isPaid(o){return String(o.payment||"").toLowerCase()==="оплачено"||o.paid===true}
function drawOrders(){
 if(!$("order-list"))return;
 const q=$("order-search").value.toLowerCase(),st=$("order-status").value,pay=$("order-payment").value;
 const list=ordersCache.filter(o=>(!q||String(o.clientName||o.client||"").toLowerCase().includes(q)||String(o.service||"").toLowerCase().includes(q))&&(st==="Все статусы"||String(o.status||"новый")===st)&&(pay==="Любая оплата"||(pay==="Оплачено"&&isPaid(o))||(pay==="Не оплачено"&&!isPaid(o))));
 $("order-list").innerHTML='<h3>Заказов: '+list.length+'</h3>'+list.map(o=>'<article class="order-row"><b>'+esc(o.clientName||o.client||"Без клиента")+'</b><p>'+esc(o.service||"")+' · '+money(o.amount)+' · '+esc(o.status||"новый")+'</p><p><b>'+(isPaid(o)?"Оплачено":"Не оплачено")+'</b></p>'+(canManage()?'<button data-paid="'+esc(o.id)+'">'+(isPaid(o)?"Снять оплату":"Отметить оплаченным")+'</button><select data-state="'+esc(o.id)+'"><option value="">Изменить статус…</option><option>в работе</option><option>выполнен</option><option>отменён</option></select>':'')+'</article>').join("")||"<p>Заказы не найдены.</p>";
 $("order-list").querySelectorAll("[data-paid]").forEach(b=>b.onclick=async()=>{const o=ordersCache.find(x=>x.id===b.dataset.paid);await db.collection("orders").doc(o.id).update({payment:isPaid(o)?"не оплачено":"оплачено"});o.payment=isPaid(o)?"не оплачено":"оплачено";drawOrders()});
 $("order-list").querySelectorAll("[data-state]").forEach(s=>s.onchange=async()=>{if(!s.value)return;await db.collection("orders").doc(s.dataset.state).update({status:s.value});const o=ordersCache.find(x=>x.id===s.dataset.state);o.status=s.value;drawOrders()});
}
async function renderFinance(){
 $("content").innerHTML=shell("Финансы","Доходы, расходы и неоплаченные заказы.")+'<div class="notice" id="finance-summary">Загрузка…</div><div class="notice"><h3>Новая операция</h3><form id="new-transaction"><select id="tx-type"><option value="income">Доход</option><option value="expense">Расход</option></select><input id="tx-amount" type="number" min="0.01" step="0.01" placeholder="Сумма" required><input id="tx-comment" placeholder="Комментарий"><button>Добавить</button></form></div><div class="notice" id="transaction-list"></div>';
 try{
 const [ts,os]=await Promise.all([db.collection("transactions").get(),db.collection("orders").get()]);
 transactionsCache=ts.docs.map(d=>d.data());const unpaid=os.docs.map(d=>d.data()).filter(o=>!isPaid(o));
 const income=transactionsCache.filter(x=>x.type==="income"||x.type==="доход").reduce((a,x)=>a+Number(x.amount||0),0);
 const expenses=transactionsCache.filter(x=>x.type!=="income"&&x.type!=="доход").reduce((a,x)=>a+Number(x.amount||0),0);
 $("finance-summary").innerHTML='<h3>Итоги</h3><p>Доходы: '+money(income)+'</p><p>Расходы: '+money(expenses)+'</p><p><b>Баланс: '+money(income-expenses)+'</b></p><hr><p>Неоплаченные заказы: '+unpaid.length+'</p><p><b>К оплате: '+money(unpaid.reduce((a,x)=>a+Number(x.amount||0),0))+'</b></p>';
 $("transaction-list").innerHTML="<h3>История операций</h3>"+transactionsCache.map(x=>'<p>'+money(x.amount)+' · '+(x.type==="income"?"Доход":"Расход")+' · '+esc(x.comment||x.description||"")+'</p>').join("");
 }catch(e){$("finance-summary").textContent=e.message}
 $("new-transaction").onsubmit=async e=>{e.preventDefault();try{await db.collection("transactions").add({amount:Number($("tx-amount").value),type:$("tx-type").value,comment:$("tx-comment").value.trim(),createdAt:firebase.firestore.FieldValue.serverTimestamp()});renderFinance()}catch(err){$("finance-summary").textContent=err.message}};
}

async function renderClients(){
 $("content").innerHTML=shell("Клиенты","Контакты и история заказов.")+
 (canManage()?'<div class="notice"><h3>Новый клиент</h3><form id="new-client"><input id="client-name" placeholder="Имя клиента" required><input id="client-phone" placeholder="Телефон"><input id="client-email" type="email" placeholder="Email"><button>Сохранить клиента</button></form></div>':"")+'<div class="notice"><input id="client-search" placeholder="Поиск клиента"><div id="client-list">Загрузка…</div></div>';
 let clients=[];try{const snap=await db.collection("clients").get();clients=snap.docs.map(d=>({id:d.id,...d.data()}));}catch(e){$("client-list").textContent=e.message;return}
 const draw=()=>{const q=$("client-search").value.toLowerCase();const list=clients.filter(x=>(x.name||x.clientName||"").toLowerCase().includes(q)||(x.phone||"").toLowerCase().includes(q));$("client-list").innerHTML=list.map(x=>'<article class="order-row"><b>'+esc(x.name||x.clientName||"Без имени")+'</b><p>'+esc(x.phone||"")+' · '+esc(x.email||"")+'</p><p>Заказы: '+ordersCache.filter(o=>(o.clientName||o.client||"").toLowerCase()===(x.name||x.clientName||"").toLowerCase()).length+'</p></article>').join("")||"<p>Клиенты не найдены.</p>"};
 draw();$("client-search").oninput=draw;
 if($("new-client"))$("new-client").onsubmit=async e=>{e.preventDefault();try{const data={name:$("client-name").value.trim(),phone:$("client-phone").value.trim(),email:$("client-email").value.trim(),createdAt:firebase.firestore.FieldValue.serverTimestamp()};const d=await db.collection("clients").add(data);clients.unshift({id:d.id,...data});e.target.reset();draw()}catch(err){$("client-list").textContent=err.message}};
}
async function renderArchive(){
 $("content").innerHTML=shell("Архив заказов","Выполненные и отменённые заказы.")+'<div class="notice"><input id="archive-search" placeholder="Поиск по архиву"><div id="archive-list">Загрузка…</div></div>';
 try{const snap=await db.collection("orders").get();const list=snap.docs.map(d=>({id:d.id,...d.data()})).filter(o=>["выполнен","отменён"].includes(String(o.status||"").toLowerCase()));const draw=()=>{const q=$("archive-search").value.toLowerCase();const a=list.filter(o=>(o.clientName||o.client||"").toLowerCase().includes(q)||(o.service||"").toLowerCase().includes(q));$("archive-list").innerHTML="<h3>В архиве: "+a.length+"</h3>"+a.map(o=>'<article class="order-row"><b>'+esc(o.clientName||o.client||"Без клиента")+'</b><p>'+esc(o.service||"")+' · '+money(o.amount)+'</p><p>'+esc(o.status)+' · '+(isPaid(o)?"Оплачено":"Не оплачено")+'</p><button data-restore="'+esc(o.id)+'">Вернуть в работу</button></article>').join("")||"<p>Архив пуст.</p>";$("archive-list").querySelectorAll("[data-restore]").forEach(b=>b.onclick=async()=>{await db.collection("orders").doc(b.dataset.restore).update({status:"в работе"});renderArchive()})};draw();$("archive-search").oninput=draw}catch(e){$("archive-list").textContent=e.message}
}
async function renderChat(){
 $("content").innerHTML=shell("Общий чат","Сообщения и вложения доступны участникам приложения.")+'<div class="notice"><div id="chat-list">Загрузка…</div><form id="chat-send"><input id="chat-text" placeholder="Напишите сообщение"><input id="chat-file" type="file" accept="image/*,video/*"><button>Отправить</button></form><p id="chat-error"></p></div>';
 const draw=snap=>{$("chat-list").innerHTML=snap.docs.map(d=>{const m=d.data();return '<article class="order-row"><b>'+esc(m.senderEmail||"Сотрудник")+'</b><p>'+esc(m.text||"")+'</p>'+(m.mediaUrl? (m.type==="video"?'<video controls src="'+esc(m.mediaUrl)+'" style="max-width:100%"></video>':'<a target="_blank" rel="noopener" href="'+esc(m.mediaUrl)+'"><img src="'+esc(m.mediaUrl)+'" alt="Вложение" style="max-width:220px;border-radius:12px"></a>'):"")+'</article>'}).join("")||"<p>Пока нет сообщений.</p>"};
 const unsub=db.collection("chat").orderBy("createdAt").onSnapshot(draw,e=>$("chat-error").textContent=e.message);
 $("chat-send").onsubmit=async e=>{e.preventDefault();const msg=$("chat-text").value.trim(),file=$("chat-file").files[0];if(!msg&&!file)return;const btn=e.submitter;btn.disabled=true;try{let mediaUrl="",type="text";if(file){if(!firebase.storage)throw new Error("Firebase Storage не подключён.");type=file.type.startsWith("video/")?"video":"image";const ref=firebase.storage().ref().child("chat/"+user.uid+"/"+Date.now()+"_"+file.name);await ref.put(file);mediaUrl=await ref.getDownloadURL()}await db.collection("chat").add({text:msg,type,mediaUrl,senderUid:user.uid,senderEmail:user.email,createdAt:firebase.firestore.FieldValue.serverTimestamp()});e.target.reset()}catch(err){$("chat-error").textContent=err.message}finally{btn.disabled=false}};
 window.addEventListener("beforeunload",()=>unsub(),{once:true});
}
function renderSettings(){
 $("content").innerHTML=shell("Настройки","Параметры веб-интерфейса.")+'<div class="notice"><label><input type="checkbox" id="dark-mode" style="min-width:0"> Тёмная тема</label><p>Аккаунт: '+esc(user.email)+'</p><p>Данные хранятся в Firebase проекта Деревяшкино.</p><button id="settings-exit">Выйти из аккаунта</button></div>';
 $("dark-mode").checked=document.body.classList.contains("dark");$("dark-mode").onchange=e=>{document.body.classList.toggle("dark",e.target.checked);try{localStorage.setItem("d22-dark",e.target.checked?"1":"0")}catch(_){}};
 $("settings-exit").onclick=()=>auth.signOut();
}
try{if(localStorage.getItem("d22-dark")==="1")document.body.classList.add("dark")}catch(_){}

document.querySelectorAll("[data-page]").forEach(b=>b.onclick=()=>page(b.dataset.page));document.addEventListener("click",e=>{if(e.target.dataset.go)page(e.target.dataset.go)});
