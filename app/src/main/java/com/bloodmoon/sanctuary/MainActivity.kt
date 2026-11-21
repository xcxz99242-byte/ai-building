package com.bloodmoon.sanctuary

import android.app.Activity
import android.os.Bundle
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.content.Context
import kotlin.math.*
import kotlin.random.Random

class MainActivity : Activity() {
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState); window.decorView.systemUiVisibility=5894 or 1024 or 512; setContentView(GameView(this)) }
}
private data class TD(val name:String,val cost:Int,val range:Float,val dmg:Float,val rate:Float,val color:Int)
private data class Tower(val x:Float,val y:Float,val type:Int,var level:Int=1,var bonus:Float=0f,var speedBonus:Float=0f,var cooldown:Float=0f)
private data class Mob(var x:Float,var y:Float,var hp:Float,val max:Float,val speed:Float,val kind:Int,var segment:Int=0)
private data class Bolt(val mob:Mob,val damage:Float,val color:Int)

class GameView(ctx:Context):View(ctx) {
 private val ink=Paint(3); private val label=Paint(3).apply{typeface=Typeface.create("serif",Typeface.BOLD)}
 private fun col(s:String)=Color.parseColor(s)
 private val defs=listOf(
  TD("乌鸦哨塔",80,118f,18f,1.05f,col("#b5a184")),TD("墓铁弩炮",125,148f,43f,.55f,col("#9ba5b4")),
  TD("血晶术塔",160,136f,27f,.8f,col("#c34c72")),TD("瘟疫炼金塔",190,108f,15f,.48f,col("#8cae79")),
  TD("圣焰灯塔",220,126f,38f,.66f,col("#dfa05e")),TD("寒霜圣龛",240,152f,22f,.9f,col("#8eb9d0")),
  TD("影界裂隙",285,170f,52f,.35f,col("#a18bc8")),TD("骨龙炮台",360,190f,75f,.28f,col("#d2c7ac")))
 private val route=listOf(.02f to .25f,.28f to .25f,.28f to .72f,.53f to .72f,.53f to .38f,.78f to .38f,.78f to .78f,.98f to .78f)
 private val towers=mutableListOf<Tower>(); private val mobs=mutableListOf<Mob>(); private val bolts=mutableListOf<Bolt>()
 private var state="menu"; private var paused=false; private var gold=360; private var hp=20; private var wave=0; private var spawned=0; private var timer=0f; private var selected=-1; private var kills=0; private var score=0; private var last=System.nanoTime()
 private fun box(c:Canvas,x:Float,y:Float,w:Float,h:Float,color:Int,r:Float=5f,outline:Boolean=false){ink.color=color;ink.style=if(outline)Paint.Style.STROKE else Paint.Style.FILL;ink.strokeWidth=2f;c.drawRoundRect(x,y,x+w,y+h,r,r,ink);ink.style=Paint.Style.FILL}
 private fun text(c:Canvas,s:String,x:Float,y:Float,size:Float,color:Int=Color.WHITE,center:Boolean=false){label.color=color;label.textSize=size;label.textAlign=if(center)Paint.Align.CENTER else Paint.Align.LEFT;label.setShadowLayer(3f,1f,2f,Color.BLACK);c.drawText(s,x,y,label);label.clearShadowLayer()}
 override fun onDraw(c:Canvas){super.onDraw(c);c.save();c.scale(width/960f,height/540f);box(c,0f,0f,960f,540f,col("#100d17"),0f);for(i in 0..22){val x=(i*83%960).toFloat();val y=(i*131%540).toFloat();ink.color=0x332d2638;c.drawCircle(x,y,18f+(i%4)*5,ink)}
 box(c,8f,7f,944f,526f,col("#80604b"),9f,true)
 when(state){"menu"->menu(c);"story"->story(c);else->{battle(c);if(paused)pause(c);if(state=="win"||state=="over")result(c)}}
 c.restore()
 if(state=="battle"&&!paused){val now=System.nanoTime();val dt=((now-last)/1e9).toFloat().coerceIn(0f,.05f);last=now;tick(dt);postInvalidateDelayed(16)}
 }
 private fun menu(c:Canvas){text(c,"血 月 圣 域",480f,150f,58f,col("#e0cbaa"),true);text(c,"LAST WATCH OF THE SANCTUARY",480f,184f,15f,col("#9f8290"),true);box(c,300f,225f,360f,62f,col("#432637"));box(c,300f,225f,360f,62f,col("#b28a68"),8f,true);text(c,"开始守卫",480f,265f,28f,col("#ead9c1"),true);text(c,"战役 · 第一章：乌鸦之门",480f,340f,20f,col("#c6b6c4"),true);text(c,"点击进入",480f,425f,17f,col("#887887"),true)}
 private fun story(c:Canvas){text(c,"第一章  乌鸦之门",480f,130f,36f,col("#d7c1a2"),true);text(c,"圣廷的最后一道封印，在今夜碎裂。",480f,225f,23f,Color.WHITE,true);text(c,"墓园深处传来铁链拖行的声音，亡者沿古道逼近。",480f,270f,19f,col("#c6b7bf"),true);text(c,"守望者，点燃哨塔。不要让它们越过圣坛。",480f,315f,19f,col("#c6b7bf"),true);box(c,390f,390f,180f,55f,col("#492637"));text(c,"进入战场",480f,425f,22f,Color.WHITE,true)}
 private fun battle(c:Canvas){box(c,20f,75f,920f,410f,col("#171722"));for(i in 0..16)for(j in 0..7)box(c,24f+i*54f,79f+j*50f,51f,47f,if((i+j)%2==0)col("#20202a") else col("#1b1b25"),2f)
 val path=Path();path.moveTo(route[0].first*920+20,route[0].second*410+75);route.drop(1).forEach{path.lineTo(it.first*920+20,it.second*410+75)};ink.color=col("#54404b");ink.strokeWidth=31f;ink.strokeCap=Paint.Cap.ROUND;ink.strokeJoin=Paint.Join.ROUND;c.drawPath(path,ink);ink.color=col("#8a6871");ink.strokeWidth=2f;ink.pathEffect=DashPathEffect(floatArrayOf(7f,9f),0f);c.drawPath(path,ink);ink.pathEffect=null
 for(i in 0..10){val x=65f+i*79f;val y=110f+(i%3)*112f;ink.color=0x664d4050;c.drawCircle(x,y,10f,ink);c.drawCircle(x+8,y+7,5f,ink)}
 towers.forEach{drawTower(c,it)};mobs.forEach{drawMob(c,it)}
 box(c,0f,0f,960f,62f,col("#211827"),0f);text(c,"生命  $hp",24f,38f,21f,col("#d8c5a5"));text(c,"金币  $gold",160f,38f,21f,col("#e0bd70"));text(c,"波次  ${wave.coerceAtLeast(1)} / 20",320f,38f,21f);text(c,"击杀  $kills",520f,38f,18f,col("#c4a6b1"))
 box(c,18f,492f,924f,42f,col("#211827"));defs.forEachIndexed{i,d->val x=22f+i*115f;box(c,x,497f,110f,32f,if(selected==i)col("#68404c") else col("#392b3a"));text(c,"${i+1} ${d.name} ${d.cost}",x+5f,518f,11f,col("#e2d2c3"))}
 if(selected>=0&&selected<towers.size){val t=towers[selected];box(c,680f,72f,260f,115f,col("#302333"));text(c,"${defs[t.type].name} Lv.${t.level}",696f,98f,17f,col("#ead9c1"));text(c,"伤害升级 45G",696f,130f,15f);text(c,"射速升级 55G",696f,158f,15f)}
 box(c,870f,8f,72f,43f,col("#4a2938"));text(c,"暂停",906f,36f,19f,Color.WHITE,true)
 }
 private fun drawTower(c:Canvas,t:Tower){val d=defs[t.type];ink.color=0x553f273b;c.drawCircle(t.x,t.y,26f,ink);ink.color=d.color;c.drawRect(t.x-12,t.y-17,t.x+12,t.y+15,ink);ink.color=col("#2b202b");c.drawRect(t.x-18,t.y+9,t.x+18,t.y+17,ink);ink.color=col("#d8c4a1");c.drawCircle(t.x,t.y-18,5f,ink);text(c,"${t.level}",t.x+17,t.y-16,12f,col("#f0d28b"),true)}
 private fun drawMob(c:Canvas,e:Mob){val colors=listOf("#c9c0ae","#777b86","#77704f","#7c9b70","#a28cbb","#8c4b68","#a18a69","#8b394f","#b89b6b","#c34c72");ink.color=0xff100d15.toInt();c.drawCircle(e.x,e.y+7,14f,ink);ink.color=col(colors[e.kind%colors.size]);c.drawCircle(e.x,e.y,if(e.kind>=8)17f else 11f,ink);ink.color=col("#2a1c28");c.drawCircle(e.x-4,e.y-2,2f,ink);c.drawCircle(e.x+4,e.y-2,2f,ink);box(c,e.x-15,e.y-19,30f,4f,col("#401d2c"),1f);box(c,e.x-15,e.y-19,30f*(e.hp/e.max).coerceIn(0f,1f),4f,col("#b74857"),1f)}
 private fun pause(c:Canvas){box(c,0f,0f,960f,540f,0x99000000.toInt(),0f);box(c,320f,170f,320f,210f,col("#2a202e"));box(c,320f,170f,320f,210f,col("#a78468"),10f,true);text(c,"暂停",480f,218f,32f,col("#e4d0b6"),true);text(c,"继续游戏",480f,275f,22f,Color.WHITE,true);text(c,"重新开始",480f,320f,20f,col("#d8b7a4"),true)}
 private fun result(c:Canvas){box(c,0f,0f,960f,540f,0xaa08060c.toInt(),0f);box(c,280f,125f,400f,290f,col("#2a202e"));box(c,280f,125f,400f,290f,col("#b18a68"),12f,true);text(c,if(state=="win")"圣域暂得安宁" else "圣域陷落",480f,190f,34f,col("#e1c9a7"),true);text(c,"击杀 $kills   得分 $score   剩余生命 $hp",480f,245f,18f,Color.WHITE,true);text(c,"点击任意位置重新开始",480f,330f,19f,col("#d8b7a4"),true)}
 private fun start(){towers.clear();mobs.clear();bolts.clear();gold=360;hp=20;wave=1;spawned=0;timer=0f;kills=0;score=0;selected=-1;paused=false;state="battle";last=System.nanoTime()}
 private fun spawn(){val boss=wave%5==0&&spawned==9;val kind=if(boss)8+wave/5 else Random.nextInt(min(8,2+wave/2));val maxHp=(38+wave*14).toFloat()*(if(boss)5f else 1f);mobs.add(Mob(38f,177.5f,maxHp,maxHp,if(boss)22f else 34f+wave*1.2f,kind))}
 private fun tick(dt:Float){timer+=dt;if(spawned<min(8+wave*2,30)&&timer>.9f){spawn();spawned++;timer=0f}
  val points=route.map{(it.first*920+20) to (it.second*410+75)};val dead=mutableListOf<Mob>()
  mobs.forEach{e->var dist=e.speed*dt;while(dist>0&&e.segment<points.size-1){val a=points[e.segment];val b=points[e.segment+1];val dx=b.first-e.x;val dy=b.second-e.y;val len=hypot(dx,dy);if(len<dist+1){e.x=b.first;e.y=b.second;e.segment++;dist-=len}else{e.x+=dx/len*dist;e.y+=dy/len*dist;dist=0f}};if(e.segment>=points.size-1){hp--;dead.add(e)}}
  mobs.removeAll(dead)
  towers.forEach{t->val d=defs[t.type];val target=mobs.filter{hypot(it.x-t.x,it.y-t.y)<=d.range}.minByOrNull{it.segment};if(target!=null){t.cooldown-=dt;if(t.cooldown<=0){target.hp-=d.dmg+t.bonus;t.cooldown=1f/(d.rate+t.speedBonus);if(target.hp<=0){mobs.remove(target);gold+=if(target.kind>=8)90 else 16;kills++;score+=25}}}}
  if(spawned>=min(8+wave*2,30)&&mobs.isEmpty()){if(wave>=20)state="win" else{wave++;spawned=0;gold+=35}}
  if(hp<=0)state="over"
 }
 override fun onTouchEvent(e:MotionEvent):Boolean{if(e.action!=MotionEvent.ACTION_UP)return true;val x=e.x/(width/960f);val y=e.y/(height/540f)
 when(state){"menu"->state="story";"story"->start();"win","over"->start();"battle"->{if(paused){if(y in 245f..295f)paused=false else if(y in 296f..350f)start()}else if(x>860&&y<65)paused=true else if(y>492){val i=((x-22f)/115f).toInt();if(i in defs.indices)selected=i}else if(selected>=0&&x>675&&y<190){val t=towers.getOrNull(selected);if(t!=null){if(y<140&&gold>=45){gold-=45;t.bonus+=defs[t.type].dmg*.55f;t.level++}else if(gold>=55){gold-=55;t.speedBonus+=.25f;t.level++}}}else if(y in 75f..485f&&selected>=0){val d=defs[selected];if(gold>=d.cost&&towers.none{hypot(it.x-x,it.y-y)<35f}){gold-=d.cost;towers.add(Tower(x,y,selected));selected=-1}else{val i=towers.indices.minByOrNull{hypot(towers[it].x-x,towers[it].y-y)};if(i!=null&&hypot(towers[i].x-x,towers[i].y-y)<32f)selected=i}}}}
 invalidate();return true}
}
