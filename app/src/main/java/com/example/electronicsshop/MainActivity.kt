package com.example.electronicsshop

import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.journeyapps.barcodescanner.IntentIntegrator
import com.journeyapps.barcodescanner.IntentResult
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

private data class Product(var id:Int,var name:String,var category:String,var buy:Double,var sell:Double,var qty:Int,var barcode:String,var imei:String="")
private data class Customer(var id:Int,var name:String,var phone:String,var debt:Double)
private data class Repair(var id:Int,var customer:String,var device:String,var fault:String,var status:String,var cost:Double)
private data class Sale(var id:Int,var productId:Int,var productName:String,var qty:Int,var total:Double,var paid:Double,var date:String,var customer:String="")

class MainActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences("shop", MODE_PRIVATE) }
    private val blue = Color.rgb(18,92,190); private val dark=Color.rgb(25,35,55); private val bg=Color.rgb(246,248,252)
    private val products=mutableListOf<Product>(); private val customers=mutableListOf<Customer>(); private val repairs=mutableListOf<Repair>(); private val sales=mutableListOf<Sale>()
    private var expenses=0.0
    private var nextId=1
    private var storeName="shop 2 mahdisat"
    private var storePhone=""
    override fun onCreate(b:Bundle?) { installSplashScreen(); super.onCreate(b); load(); dashboard() }

    private fun load(){
        products.clear(); customers.clear(); repairs.clear(); sales.clear();
        fun arr(key:String)=JSONArray(prefs.getString(key,"[]"))
        val p=arr("products"); for(i in 0 until p.length()){val o=p.getJSONObject(i);products.add(Product(o.getInt("id"),o.getString("name"),o.getString("cat"),o.getDouble("buy"),o.getDouble("sell"),o.getInt("qty"),o.optString("bar"),o.optString("imei")))}
        val c=arr("customers"); for(i in 0 until c.length()){val o=c.getJSONObject(i);customers.add(Customer(o.getInt("id"),o.getString("name"),o.getString("phone"),o.getDouble("debt")))}
        val r=arr("repairs"); for(i in 0 until r.length()){val o=r.getJSONObject(i);repairs.add(Repair(o.getInt("id"),o.getString("customer"),o.getString("device"),o.getString("fault"),o.getString("status"),o.getDouble("cost")))}
        val s=arr("sales"); for(i in 0 until s.length()){val o=s.getJSONObject(i);sales.add(Sale(o.getInt("id"),o.getInt("pid"),o.getString("name"),o.getInt("qty"),o.getDouble("total"),o.getDouble("paid"),o.getString("date"),o.optString("customer")))}
        expenses=prefs.getString("expenses","0")!!.toDoubleOrNull()?:0.0
        nextId=prefs.getInt("nextId",1)
        storeName=prefs.getString("storeName","shop 2 mahdisat") ?: "shop 2 mahdisat"
        storePhone=prefs.getString("storePhone","") ?: ""
    }
    private fun save(){
        fun ps():JSONArray{val a=JSONArray();products.forEach{a.put(JSONObject().apply{put("id",it.id);put("name",it.name);put("cat",it.category);put("buy",it.buy);put("sell",it.sell);put("qty",it.qty);put("bar",it.barcode);put("imei",it.imei)})};return a}
        fun cs():JSONArray{val a=JSONArray();customers.forEach{a.put(JSONObject().apply{put("id",it.id);put("name",it.name);put("phone",it.phone);put("debt",it.debt)})};return a}
        fun rs():JSONArray{val a=JSONArray();repairs.forEach{a.put(JSONObject().apply{put("id",it.id);put("customer",it.customer);put("device",it.device);put("fault",it.fault);put("status",it.status);put("cost",it.cost)})};return a}
        fun ss():JSONArray{val a=JSONArray();sales.forEach{a.put(JSONObject().apply{put("id",it.id);put("pid",it.productId);put("name",it.productName);put("qty",it.qty);put("total",it.total);put("paid",it.paid);put("date",it.date);put("customer",it.customer)})};return a}
        prefs.edit().putString("products",ps().toString()).putString("customers",cs().toString()).putString("repairs",rs().toString()).putString("sales",ss().toString()).putString("expenses",expenses.toString()).putInt("nextId",nextId).putString("storeName",storeName).putString("storePhone",storePhone).apply()
    }
    private fun today():String=SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(Date())
    private fun todaySales()=sales.filter{it.date==today()}.sumOf{it.total}
    private fun todayProfit()=sales.filter{it.date==today()}.sumOf{s->s.total-(products.find{it.id==s.productId}?.buy?:0.0)*s.qty}

    private fun root(title:String,back:Boolean=true):LinearLayout{
        val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(18,18,18,12);layoutDirection=View.LAYOUT_DIRECTION_RTL}
        val bar=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        if(back) bar.addView(Button(this).apply{text="‹";setOnClickListener{dashboard()}} ,LinearLayout.LayoutParams(60,60))
        bar.addView(TextView(this).apply{text=title;textSize=23f;setTypeface(null,Typeface.BOLD);setTextColor(dark);gravity=Gravity.CENTER;},LinearLayout.LayoutParams(0,60,1f))
        l.addView(bar)
        return l
    }
    private fun btn(t:String,action:()->Unit)=Button(this).apply{text=t;textSize=16f;setOnClickListener{action()}}
    private fun card(t:String):TextView=TextView(this).apply{text=t;textSize=17f;setTextColor(dark);gravity=Gravity.CENTER;setPadding(12,20,12,20);setBackgroundColor(Color.WHITE)}

    private fun dashboard(){
        val l=root("shop 2 mahdisat",false)
        val logo=ImageView(this).apply{setImageResource(com.example.electronicsshop.R.drawable.shop2_mahdisat_logo);adjustViewBounds=true;scaleType=ImageView.ScaleType.CENTER_INSIDE}
        l.addView(logo,LinearLayout.LayoutParams(-1,220))
        l.addView(card("$storeName\nالمبيعات اليوم: ${money(todaySales())} دج\nالربح التقريبي: ${money(todayProfit())} دج"))
        val grid=GridLayout(this).apply{columnCount=2}
        val items=listOf("🛒 المبيعات" to {salesScreen()},"📦 المنتجات" to {productsScreen()},"👥 الزبائن والديون" to {customersScreen()},"🔧 الصيانة" to {repairsScreen()},"💰 الصندوق" to {cashScreen()},"📊 التقارير" to {reportsScreen()},"⚙️ الإعدادات" to {settingsScreen()})
        items.forEach{(t,a)->grid.addView(btn(t,a),GridLayout.LayoutParams().apply{width=0;height=100;columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);setMargins(5,5,5,5)})}
        l.addView(grid)
        l.addView(card("المخزون الناقص: ${products.count{it.qty<=2}}   |   ديون الزبائن: ${money(customers.sumOf{it.debt})} دج"))
        val s=ScrollView(this);s.addView(l);setContentView(s)
    }

    private fun productsScreen(){
        val l=root("المنتجات والمخزون");l.addView(btn("＋ إضافة منتج"){productDialog(null)});l.addView(btn("▣ مسح باركود"){scanBarcode()})
        if(products.isEmpty())l.addView(card("لا توجد منتجات. أضف أول منتج."))
        products.forEach{p-> val row=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(14,12,14,12);setBackgroundColor(Color.WHITE)};row.addView(TextView(this).apply{text="${p.name}  •  ${money(p.sell)} دج";textSize=18f;setTypeface(null,Typeface.BOLD)});row.addView(TextView(this).apply{text="${p.category} | الكمية: ${p.qty} | شراء: ${money(p.buy)} دج | باركود: ${p.barcode.ifBlank{"—"}} | IMEI: ${p.imei.ifBlank{"—"}}";textSize=14f});row.addView(btn("تعديل / حذف"){productDialog(p)});l.addView(row,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,5,0,5)})}
        setContentView(ScrollView(this).apply{addView(l)})
    }
    private fun productDialog(old:Product?, prefillBarcode:String=""){
        val form=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(12,12,12,12)}
        val n=EditText(this);n.hint="اسم المنتج";n.setText(old?.name?:(""));val cat=EditText(this);cat.hint="التصنيف";cat.setText(old?.category?:"");val buy=EditText(this);buy.hint="سعر الشراء";buy.inputType=2;buy.setText(old?.buy?.toString()?:(""));val sell=EditText(this);sell.hint="سعر البيع";sell.inputType=2;sell.setText(old?.sell?.toString()?:(""));val qty=EditText(this);qty.hint="الكمية";qty.inputType=2;qty.setText(old?.qty?.toString()?:(""));val bar=EditText(this);bar.hint="الباركود";bar.setText(old?.barcode?.ifBlank{prefillBarcode}?:prefillBarcode);val imei=EditText(this);imei.hint="IMEI / الرقم التسلسلي (اختياري)";imei.setText(old?.imei?:"");listOf(n,cat,buy,sell,qty,bar,imei).forEach{form.addView(it)}
        AlertDialog.Builder(this).setTitle(if(old==null)"إضافة منتج" else "تعديل المنتج").setView(form).setPositiveButton("حفظ"){_,_->
            val p=Product(old?.id?:nextId++,n.text.toString(),cat.text.toString(),buy.text.toString().toDoubleOrNull()?:0.0,sell.text.toString().toDoubleOrNull()?:0.0,qty.text.toString().toIntOrNull()?:0,bar.text.toString(),imei.text.toString());if(old==null)products.add(p) else {val i=products.indexOfFirst{it.id==old.id};products[i]=p};save();productsScreen()
        }.apply{if(old!=null)setNegativeButton("حذف"){_,_->products.removeIf{it.id==old.id};save();productsScreen()}}.setNeutralButton("إلغاء",null).show()
    }

    private fun salesScreen(){
        val l=root("المبيعات");l.addView(btn("＋ تسجيل بيع"){saleDialog()});val total=sales.sumOf{it.total};l.addView(card("إجمالي المبيعات: ${money(total)} دج"));sales.asReversed().forEach{s->val c=card("${s.productName} × ${s.qty}\n${money(s.total)} دج | مدفوع: ${money(s.paid)} دج\n${s.date}");c.setOnClickListener{invoiceDialog(s)};l.addView(c)};setContentView(ScrollView(this).apply{addView(l)})
    }
    private fun saleDialog(){if(products.none{it.qty>0}){toast("لا توجد منتجات متوفرة للبيع");return};val names=products.filter{it.qty>0}.map{it.name}.toTypedArray();val form=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(12,12,12,12)};val sp=Spinner(this);sp.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,names);val q=EditText(this);q.hint="الكمية";q.inputType=2;q.setText("1");val paid=EditText(this);paid.hint="المبلغ المدفوع";paid.inputType=2;val cust=EditText(this);cust.hint="اسم الزبون (اختياري)";form.addView(sp);form.addView(q);form.addView(paid);form.addView(cust);AlertDialog.Builder(this).setTitle("تسجيل بيع").setView(form).setPositiveButton("إتمام البيع"){_,_->val p=products.filter{it.qty>0}[sp.selectedItemPosition];val qq=q.text.toString().toIntOrNull()?:1;if(qq<=0||qq>p.qty){toast("الكمية غير متوفرة");return@setPositiveButton};val tot=p.sell*qq;val pay=paid.text.toString().toDoubleOrNull()?:tot;p.qty-=qq;val customerName=cust.text.toString();sales.add(Sale(nextId++,p.id,p.name,qq,tot,pay,today(),customerName));if(customerName.isNotBlank() && pay<tot){val c=customers.find{it.name.trim()==customerName.trim()};if(c!=null)c.debt+=(tot-pay) else customers.add(Customer(nextId++,customerName,"",tot-pay))};save();salesScreen()}.setNegativeButton("إلغاء",null).show()}

    private fun customersScreen(){val l=root("الزبائن والديون");l.addView(btn("＋ إضافة زبون"){customerDialog(null)});l.addView(card("إجمالي الديون: ${money(customers.sumOf{it.debt})} دج"));customers.forEach{c->val t=TextView(this).apply{text="${c.name}\n${c.phone}\nالدين: ${money(c.debt)} دج";textSize=17f;setPadding(15,15,15,15);setBackgroundColor(Color.WHITE);setOnClickListener{customerDialog(c)}};l.addView(t,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,5,0,5)})};setContentView(ScrollView(this).apply{addView(l)})}
    private fun customerDialog(old:Customer?){val f=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(12,12,12,12)};val n=EditText(this);n.hint="الاسم";n.setText(old?.name?:"");val ph=EditText(this);ph.hint="الهاتف";ph.setText(old?.phone?:"");val d=EditText(this);d.hint="الدين";d.inputType=2;d.setText(old?.debt?.toString()?:("0"));f.addView(n);f.addView(ph);f.addView(d);AlertDialog.Builder(this).setTitle("بيانات الزبون").setView(f).setPositiveButton("حفظ"){_,_->val c=Customer(old?.id?:nextId++,n.text.toString(),ph.text.toString(),d.text.toString().toDoubleOrNull()?:0.0);if(old==null)customers.add(c)else customers[customers.indexOfFirst{it.id==old.id}]=c;save();customersScreen()}.setNegativeButton("إلغاء",null).show()}

    private fun repairsScreen(){val l=root("الصيانة");l.addView(btn("＋ إضافة جهاز للصيانة"){repairDialog()});repairs.forEach{r->l.addView(card("${r.device} — ${r.customer}\nالعطل: ${r.fault}\nالحالة: ${r.status} | التكلفة: ${money(r.cost)} دج"))};setContentView(ScrollView(this).apply{addView(l)})}
    private fun repairDialog(){val f=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(12,12,12,12)};val n=EditText(this);n.hint="اسم الزبون";val d=EditText(this);d.hint="الجهاز";val fa=EditText(this);fa.hint="العطل";val st=EditText(this);st.hint="الحالة";st.setText("قيد الإصلاح");val co=EditText(this);co.hint="التكلفة";co.inputType=2;listOf(n,d,fa,st,co).forEach{f.addView(it)};AlertDialog.Builder(this).setTitle("استلام جهاز").setView(f).setPositiveButton("حفظ"){_,_->repairs.add(Repair(nextId++,n.text.toString(),d.text.toString(),fa.text.toString(),st.text.toString(),co.text.toString().toDoubleOrNull()?:0.0));save();repairsScreen()}.setNegativeButton("إلغاء",null).show()}

    private fun cashScreen(){val l=root("الصندوق");l.addView(card("المبيعات: ${money(sales.sumOf{it.total})} دج\nالمصاريف: ${money(expenses)} دج\nالرصيد التقريبي: ${money(sales.sumOf{it.paid}-expenses)} دج"));l.addView(btn("＋ إضافة مصروف"){val e=EditText(this);e.hint="قيمة المصروف";e.inputType=2;AlertDialog.Builder(this).setTitle("مصروف جديد").setView(e).setPositiveButton("حفظ"){_,_->expenses+=e.text.toString().toDoubleOrNull()?:0.0;save();cashScreen()}.setNegativeButton("إلغاء",null).show()});setContentView(l)}
    private fun reportsScreen(){val l=root("التقارير");l.addView(card("عدد المنتجات: ${products.size}\nقيمة المخزون بسعر الشراء: ${money(products.sumOf{it.buy*it.qty})} دج\nقيمة المخزون بسعر البيع: ${money(products.sumOf{it.sell*it.qty})} دج\nإجمالي المبيعات: ${money(sales.sumOf{it.total})} دج\nإجمالي الديون: ${money(customers.sumOf{it.debt})} دج\nأجهزة قيد الصيانة: ${repairs.count{it.status!="مكتمل"}}"));setContentView(l)}
    private fun settingsScreen(){
        val l=root("الإعدادات والنسخ الاحتياطي")
        l.addView(btn("🏪 بيانات المحل"){storeDialog()})
        l.addView(btn("💾 إنشاء نسخة احتياطية"){backupData()})
        l.addView(btn("📂 استرجاع نسخة احتياطية"){restoreData()})
        l.addView(btn("ℹ️ حول التطبيق"){AlertDialog.Builder(this).setTitle("shop 2 mahdisat").setMessage("shop 2 mahdisat — تطبيق تسيير محل إلكترونيات — النسخة V5\nالبيانات محفوظة محليًا على الهاتف.").setPositiveButton("حسنًا",null).show()})
        setContentView(ScrollView(this).apply{addView(l)})
    }

    private fun invoiceDialog(s:Sale){
        val invoice=invoiceText(s)
        AlertDialog.Builder(this).setTitle("فاتورة #${s.id}").setMessage(invoice)
            .setPositiveButton("مشاركة"){_,_->shareText("فاتورة بيع #${s.id}",invoice)}
            .setNegativeButton("طباعة"){_,_->printText("فاتورة #${s.id}",invoice)}
            .setNeutralButton("إغلاق",null).show()
    }

    private fun invoiceText(s:Sale):String = """
        ${storeName}
        -------------------------
        فاتورة بيع رقم: ${s.id}
        التاريخ: ${s.date}
        الزبون: ${s.customer.ifBlank{"—"}}
        المنتج: ${s.productName}
        الكمية: ${s.qty}
        الإجمالي: ${money(s.total)} دج
        المدفوع: ${money(s.paid)} دج
        الباقي: ${money(s.total-s.paid)} دج
        -------------------------
        ${if(storePhone.isNotBlank()) "الهاتف: $storePhone\n        " else ""}شكرًا لزيارتكم
    """.trimIndent()

    private fun shareText(title:String,text:String){
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,text)},title))
    }

    private fun printText(title:String,text:String){
        val printManager=getSystemService(PRINT_SERVICE) as android.print.PrintManager
        printManager.print(title, object: android.print.PrintDocumentAdapter(){
            override fun onLayout(oldAttributes: android.print.PrintAttributes?, newAttributes: android.print.PrintAttributes?, cancellationSignal: android.os.CancellationSignal?, callback: android.print.PrintDocumentAdapter.LayoutResultCallback?, extras: Bundle?){
                if(cancellationSignal?.isCanceled==true){callback?.onLayoutCancelled();return}
                val info=android.print.PrintDocumentInfo.Builder("$title.pdf").setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(1).build()
                callback?.onLayoutFinished(info,true)
            }
            override fun onWrite(pages: android.print.PageRange?, destination: android.os.ParcelFileDescriptor?, cancellationSignal: android.os.CancellationSignal?, callback: android.print.PrintDocumentAdapter.WriteResultCallback?){
                try{
                    val out=java.io.FileOutputStream(destination?.fileDescriptor)
                    val pdf=android.graphics.pdf.PdfDocument();val page=pdf.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595,842,1).create())
                    val canvas=page.canvas;val paint=android.graphics.Paint().apply{textSize=14f};var y=50f
                    text.split("\\n").forEach{canvas.drawText(it.trim(),40f,y,paint);y+=24f};pdf.finishPage(page);pdf.writeTo(out);pdf.close();out.close();callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                }catch(e:Exception){callback?.onWriteFailed(e.message)}
            }
        },null)
    }

    private fun scanBarcode(){
        IntentIntegrator(this).setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES).setPrompt("وجّه الكاميرا نحو الباركود").setBeepEnabled(true).setOrientationLocked(false).initiateScan()
    }

    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==9002 && resultCode==RESULT_OK && data?.data!=null){writeBackup(data.data!!);return}
        if(requestCode==9001 && resultCode==RESULT_OK && data?.data!=null){importBackup(data.data!!);return}
        val r=IntentIntegrator.parseActivityResult(requestCode,resultCode,data)
        if(r!=null){
            val code=r.contents ?: return
            val p=products.find{it.barcode==code}
            if(p!=null){productDialog(p)}else{
                AlertDialog.Builder(this).setTitle("باركود جديد").setMessage("لم يتم العثور على منتج بهذا الباركود. هل تريد إضافة منتج جديد؟").setPositiveButton("إضافة"){_,_->productDialog(null,code)}.setNegativeButton("إلغاء",null).show()
            }
        }
    }

    private fun backupJson():String{
        return JSONObject().apply{
            put("products",JSONArray(prefs.getString("products","[]")))
            put("customers",JSONArray(prefs.getString("customers","[]")))
            put("repairs",JSONArray(prefs.getString("repairs","[]")))
            put("sales",JSONArray(prefs.getString("sales","[]")))
            put("expenses",expenses)
            put("nextId",nextId).put("storeName",storeName).put("storePhone",storePhone)
        }.toString(2)
    }

    private fun backupData(){
        val i=Intent(Intent.ACTION_CREATE_DOCUMENT).apply{
            type="application/json"
            putExtra(Intent.EXTRA_TITLE,"electronics_shop_backup.json")
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(i,9002)
    }

    private fun restoreData(){
        val i=Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="application/json";addCategory(Intent.CATEGORY_OPENABLE)}
        startActivityForResult(i,9001)
    }

    private fun writeBackup(uri:Uri){
        try{
            contentResolver.openOutputStream(uri)?.use{it.write(backupJson().toByteArray(Charsets.UTF_8))}
            toast("تم حفظ النسخة الاحتياطية")
        }catch(e:Exception){toast("تعذر حفظ النسخة الاحتياطية")}
    }

    private fun importBackup(uri:Uri){
        try{
            val text=contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()} ?: return
            val o=JSONObject(text)
            prefs.edit().putString("products",o.optJSONArray("products")?.toString()?:"[]").putString("customers",o.optJSONArray("customers")?.toString()?:"[]").putString("repairs",o.optJSONArray("repairs")?.toString()?:"[]").putString("sales",o.optJSONArray("sales")?.toString()?:"[]").putString("expenses",o.optDouble("expenses",0.0).toString()).putInt("nextId",o.optInt("nextId",1)).putString("storeName",o.optString("storeName","shop 2 mahdisat")).putString("storePhone",o.optString("storePhone","")).apply()
            load();toast("تم استرجاع النسخة بنجاح");dashboard()
        }catch(e:Exception){toast("تعذر استرجاع النسخة")}
    }


    private fun storeDialog(){
        val f=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(12,12,12,12)}
        val n=EditText(this);n.hint="اسم المحل";n.setText(storeName)
        val ph=EditText(this);ph.hint="رقم الهاتف";ph.setText(storePhone);ph.inputType=3
        f.addView(n);f.addView(ph)
        AlertDialog.Builder(this).setTitle("بيانات المحل").setView(f).setPositiveButton("حفظ"){_,_->storeName=n.text.toString().ifBlank{"shop 2 mahdisat"};storePhone=ph.text.toString();save();dashboard()}.setNegativeButton("إلغاء",null).show()
    }
    private fun money(x:Double)=String.format(Locale.getDefault(),"%.0f",x)
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
