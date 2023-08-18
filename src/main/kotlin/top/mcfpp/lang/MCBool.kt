package top.mcfpp.lang

import top.mcfpp.annotations.InsertCommand
import top.mcfpp.exception.VariableConverseException
import top.mcfpp.lib.*
import java.util.*
import top.mcfpp.lib.Function
import kotlin.collections.HashMap

/**
 * 布尔型变量是mcfpp的基本类型之一，它表示一个只有0，1两种取值可能性的值。
 *
 * 在实际实现过程中，它仍然是由记分板实现的，也就是说它本质仍然是一个记分板的int型变量。如果直接对mcfunction操作，你也可以对布尔型进行加减法。
 * 但是在mcfpp中你是不允许这么做的。
 *
 * bool型变量实现了多种计算方法，比如与，或，非等基本的逻辑运算。
 */
class MCBool : Var, OnScoreboard {
    /**
     * 此bool变量含有的值。仅在它为字面量时才有效。
     */
    var value = false

    /**
     * 此bool变量依托的记分板
     */
    var boolObject: SbObject = SbObject.MCS_boolean

    constructor(id: String, curr: FieldContainer) {
        identifier = id
        name = curr.prefix + "_" + id
    }

    constructor(b: Boolean, id: String, curr: FieldContainer) : this(id, curr) {
        value = b
        isConcrete = true
    }

    constructor(id: String) {
        identifier = id
        name = id
    }

    constructor() : this(UUID.randomUUID().toString())

    constructor(b: MCBool) : super(b) {
        this.value = b.value;
        this.boolObject = b.boolObject;
    }

    constructor(b: Boolean) : this(UUID.randomUUID().toString()) {
        isConcrete = true
        value = b
    }

    @get:Override
    override val type: String
        get() = "bool"

    @Override
    override fun assign(b: Var?) {
        if (b is MCBool) {
            assignCommand(b)
        } else {
            throw VariableConverseException()
        }
    }

    @Override
    override fun cast(type: String): Var? {
        return if (type == this.type) {
            this
        } else null
    }

    @InsertCommand
    fun equalCommand(a: MCBool): MCBool {
        //re = t == a
        val re: MCBool
        if (isConcrete && a.isConcrete) {
            re = MCBool(Objects.equals(value, a.value))
        } else if (isConcrete) {
            re = a.equalCommand(this)
        } else if (a.isConcrete) {
            //execute store success score qwq qwq if score qwq qwq = owo owo
            re = MCBool()
            Function.addCommand(
                "execute store success score " + re.name + " " + re.boolObject
                        + " if score " + name + " " + boolObject + " matches " + if (a.value) 1 else 0
            )
        } else {
            re = MCBool()
            Function.addCommand(
                "execute store success score " + re.name + " " + re.boolObject
                        + " if score " + name + " " + boolObject + " = " + a.name + " " + a.boolObject
            )
        }
        return re
    }

    @InsertCommand
    fun notEqualCommand(a: MCBool): MCBool {
        //re = t != a
        val re: MCBool
        if (isConcrete && a.isConcrete) {
            re = MCBool(!Objects.equals(value, a.value))
        } else if (isConcrete) {
            re = a.equalCommand(this)
        } else if (a.isConcrete) {
            //execute store success score qwq qwq if score qwq qwq = owo owo
            re = MCBool()
            Function.addCommand(
                "execute store success score " + re.name + " " + re.boolObject
                        + " unless score " + name + " " + boolObject + " matches " + if (a.value) 1 else 0
            )
        } else {
            re = MCBool()
            Function.addCommand(
                "execute store success score " + re.name + " " + re.boolObject
                        + " unless score " + name + " " + boolObject + " = " + a.name + " " + a.boolObject
            )
        }
        return re
    }

    @InsertCommand
    fun negation(): MCBool {
        if (isConcrete) {
            value = !value
        } else {
            Function.addCommand(
                "execute store success score " + name + " " + boolObject
                        + " if score " + name + " " + boolObject + " matches " + 0
            )
        }
        return this
    }

    @InsertCommand
    fun or(a: MCBool): MCBool {
        val re: MCBool
        if (isConcrete && a.isConcrete) {
            re = MCBool(value || a.value)
        } else if (isConcrete) {
            re = a.or(this)
        } else if (a.isConcrete) {
            if (a.value) {
                re = MCBool(true)
            } else {
                re = MCBool()
                Function.addCommand(
                    "execute store success score " + re.name + " " + re.boolObject
                            + " if score " + name + " " + boolObject + " matches " + 1
                )
            }
        } else {
            re = MCBool()
            Function.addCommand(
                "execute store success score " + re.name + " " + re.boolObject
                        + " if score " + name + " " + boolObject + " matches " + 1
            )
            Function.addCommand(
                "execute" +
                        " if score " + re.name + " " + re.boolObject + " matches " + 0 +
                        " store success score " + re.name + " " + re.boolObject +
                        " if score " + a.name + " " + a.boolObject + " matches " + 1
            )
        }
        return re
    }

    @InsertCommand
    fun and(a: MCBool): MCBool {
        val re: MCBool
        if (isConcrete && a.isConcrete) {
            re = MCBool(value && a.value)
        } else if (isConcrete) {
            re = a.and(this)
        } else if (a.isConcrete) {
            if (!a.value) {
                re = MCBool(false)
            } else {
                re = MCBool()
                Function.addCommand(
                    "execute store success score " + re.name + " " + re.boolObject
                            + " if score " + name + " " + boolObject + " matches " + 1
                )
            }
        } else {
            re = MCBool()
            Function.addCommand(
                "execute store success score " + re.name + " " + re.boolObject
                        + " if score " + name + " " + boolObject + " matches " + 1
            )
            Function.addCommand(
                "execute" +
                        " if score " + re.name + " " + re.boolObject + " matches " + 1 +
                        " store success score " + re.name + " " + re.boolObject +
                        " if score " + a.name + " " + a.boolObject + " matches " + 1
            )
        }
        return re
    }

    @InsertCommand
    private fun assignCommand(a: MCBool) {
        if (a.isConcrete) {
            isConcrete = true
            value = true
        } else {
            isConcrete = false
            //变量进栈
            Function.addCommand(
                "execute" +
                        " store result storage mcfpp:system " + top.mcfpp.Project.defaultNamespace + ".stack_frame[" + stackIndex + "]." + identifier + " int 1" +
                        " run scoreboard players operation " + name + " " + boolObject + " = " + a.name + " " + a.boolObject
            )
        }
    }

    @Override
    override fun clone(): MCBool {
        return MCBool(this)
    }

    @Override
    override fun setObj(sbObject: SbObject): MCBool {
        boolObject = sbObject
        return this
    }

    @Override
    @InsertCommand
    override fun getTempVar(cache: HashMap<Var, String>): MCBool {
        if(isTemp) return this
        val re : MCBool
        if(isConcrete){
            re = MCBool(value)
            re.isTemp = true
            return re
        }
        re = MCBool()
        if(isClassMember) {
            Function.addCommand(
                "execute as @e[type=marker,tag=${clsPointer!!.clsType.tag}]" +
                        "if score @s ${clsPointer!!.address.`object`.name} = ${clsPointer!!.address.name} ${clsPointer!!.address.`object`.name}" +
                        "run" +
                        "scoreboard players operation ${re.name} ${SbObject.MCS_boolean.name} = @s ${SbObject.MCS_boolean.name}"
            )
        }else{
            Function.addCommand(
                "scoreboard players operation ${re.name} ${SbObject.MCS_boolean.name} = $name ${SbObject.MCS_boolean.name}"
            )
        }
        re.isTemp = true
        return re
    }
}