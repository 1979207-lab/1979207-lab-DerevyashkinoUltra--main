const $=id=>document.getElementById(id);
let auth=null,db=null,user=null,currentPage="home",ordersCache=[],transactionsCache=[];
const esc=s=>String(s??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
const money=n=>(Number(n)||0).toLocaleString("ru-RU",{minimumFractionDigits:2,maximumFractionDigits:2})+" ₽";
if(window.firebaseConfig&&window.firebaseConfig.apiKey&&window.firebaseConfig.apiKey!=="PASTE_WEB_API_KEY"){
 firebase.initializeApp(window.firebaseConfig);auth=firebase.auth();db=firebase.firestore();
 auth.onAuthStateChanged(u=>{user=u;$("logout").hidden=!u;$("login").hidden=!!u;$("email").hidden=!!u;$("password").hidden=!!u;$("status").textContent=u?"Вы вошли как "+u.email+".":"Войдите в аккаунт.";renderPage();});
}
$("login").onclick=()=>{if(!auth){$("status").textContent="Сначала настройте web/firebase-config.js по примеру.";return}auth.signInWithEmailAndPassword($("email").value.trim(),$("password").value).catch(e=>$("status").textContent=e.message)};
$("logout").onclick=()=>auth&&auth.signOut();
function page(name){currentPage=name;document.querySelectorAll("nav button").forEach(b=>b.classList.toggle("active",b.dataset.page===name));renderPage()}
function shell(title,subtitle=""){return '<div class="hero"><p class="eyebrow">ДЕРЕВЯШКИНО 22</p><h1>'+esc(title)+'</h1><p>'+esc(subtitle)+'</p></div>'}
function renderPage(){
 const titles={home:"Главная",orders:"Заказы",clients:"Клиенты",finance:"Финансы",chat:"Чат",archive:"Архив",settings:"Настройки"};
 if(!user){$("content").innerHTML=shell("Деревяшкино 22","Войдите, чтобы работать с заказами и данными мастерской.");return}
 if(currentPage==="orders")return renderOrders();
 if(currentPage==="finance")return renderFinance();
 if(currentPage==="home"){$("content").innerHTML=shell("Деревяшкино 22","Рабочий стол мастерской")+'<div class="tiles"><button data-go="orders">Заказы →</button><button data-go="clients">Клиенты →</button><button data-go="finance">Финансы →</button><button data-go="chat">Чат →</button><button data-go="archive">Архив →</button></div>';return}
 const descriptions={clients:"Раздел клиентов пока не подключён к операциям добавления и редактирования.",chat:"Чат и отправка вложений для веб-версии ещё не реализованы.",archive:"Архив будет подключён после завершения проверки правил переноса заказов.",settings:"Настройки веб-версии."};
 $("content").innerHTML=shell(titles[currentPage],descriptions[currentPage]||"");
}
async function renderOrders(){
 $("content").innerHTML=shell("Заказы","Создание, поиск, статусы и отметка оплаты.")+
 '<div class="notice"><h3>Новый заказ</h3><form id="new-order"><input id="order-client" placeholder="Клиент" required><input id="order-service" placeholder="Услуга / изделие" required><input id="order-amount" type="number" min="0.01" step="0.01" placeholder="Сумма, ₽" required><button>Добавить заказ</button></form></div>'+
 '<div class="notice"><input id="order-search" placeholder="Поиск по клиенту или изделию"><select id="order-status"><option>Все статусы</option><option>новый</option><option>в работе</option><option>выполнен</option><option>отменён</option></select><select id="order-payment"><option>Любая оплата</option><option>Оплачено</option><option>Не оплачено</option></select><div id="order-list">Загрузка…</div></div>';
 try{const snap=await db.collection("orders").get();ordersCache=snap.docs.map(d=>({id:d.id,...d.data()}));drawOrders()}catch(e){$("order-list").textContent=e.message}
 $("new-order").onsubmit=async e=>{e.preventDefault();try{await db.collection("orders").add({clientName:$("order-client").value.trim(),service:$("order-service").value.trim(),amount:Number($("order-amount").value),status:"новый",payment:"не оплачено",createdAt:firebase.firestore.FieldValue.serverTimestamp()});renderOrders()}catch(err){$("order-list").textContent=err.message}};
 ["order-search","order-status","order-payment"].forEach(id=>$(id).addEventListener("input",drawOrders));
}
function isPaid(o){return String(o.payment||"").toLowerCase()==="оплачено"||o.paid===true}
function drawOrders(){
 if(!$("order-list"))return;
 const q=$("order-search").value.toLowerCase(),st=$("order-status").value,pay=$("order-payment").value;
 const list=ordersCache.filter(o=>(!q||String(o.clientName||o.client||"").toLowerCase().includes(q)||String(o.service||"").toLowerCase().includes(q))&&(st==="Все статусы"||String(o.status||"новый")===st)&&(pay==="Любая оплата"||(pay==="Оплачено"&&isPaid(o))||(pay==="Не оплачено"&&!isPaid(o))));
 $("order-list").innerHTML='<h3>Заказов: '+list.length+'</h3>'+list.map(o=>'<article class="order-row"><b>'+esc(o.clientName||o.client||"Без клиента")+'</b><p>'+esc(o.service||"")+' · '+money(o.amount)+' · '+esc(o.status||"новый")+'</p><p><b>'+ (isPaid(o)?"Оплачено":"Не оплачено")+'</b></p><button data-paid="'+esc(o.id)+'">'+(isPaid(o)?"Снять оплату":"Отметить оплаченным")+'</button><select data-state="'+esc(o.id)+'"><option value="">Изменить статус…</option><option>в работе</option><option>выполнен</option><option>отменён</option></select></article>').join("")||"<p>Заказы не найдены.</p>";
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
document.querySelectorAll("[data-page]").forEach(b=>b.onclick=()=>page(b.dataset.page));document.addEventListener("click",e=>{if(e.target.dataset.go)page(e.target.dataset.go)});
