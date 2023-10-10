package top.mcfpp.lib

import top.mcfpp.Project
import top.mcfpp.lang.ClassPointer
import top.mcfpp.lang.MCBool
import top.mcfpp.lang.MCInt
import top.mcfpp.lang.Var

/**
 * 函数的参数。用于函数声明的时候。
 */
class FunctionParam(
    /**
     * 参数类型
     */
    var type: String,
    /**
     * 参数的名字
     */
    var identifier: String,
    /**
     * 参数是否为静态的
     */
    var isStatic: Boolean
) {
    companion object {
        /**
         * 将一个参数列表转换为对应的字符串列表
         * @param params 参数列表
         * @return 它的字符串列表
         */
        fun toStringList(params: ArrayList<FunctionParam>): ArrayList<String> {
            val qwq: ArrayList<String> = ArrayList()
            for (param in params) {
                qwq.add(param.type)
            }
            return qwq
        }

        /**
         * 将一个变量列表转换为对应的字符串列表
         * @param params 参数列表
         * @return 它的字符串列表
         */
        fun getVarTypes(params: ArrayList<Var>): ArrayList<String> {
            val qwq: ArrayList<String> = ArrayList()
            for (param in params) {
                qwq.add(param.type)
            }
            return qwq
        }

        fun getParams(params: mcfppParser.ParameterListContext): ArrayList<String>{
            val re = ArrayList<String>()
            for (param in params.parameter()) {
                re.add(param.type().text)
            }
            return re
        }
    }
}