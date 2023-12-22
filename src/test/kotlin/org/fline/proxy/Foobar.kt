package org.fline.proxy

fun hasNodeWithName(node: MyNode, name:String):Boolean {
    if(node.name==name) return true
    var cur : MyNode?= node
    while (true){
        cur=cur?.next
        if (cur==null) return false
        if (cur.name==name) return true
    }
}

fun main(){
    val node= MyNode("3", MyNode("2", MyNode("1", null)))
    println(hasNodeWithName(node,"2"))
    println(hasNodeWithName(node,"4"))
}
