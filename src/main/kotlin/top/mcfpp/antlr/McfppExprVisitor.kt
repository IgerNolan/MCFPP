package top.mcfpp.antlr

import net.querz.nbt.io.SNBTUtil
import net.querz.nbt.tag.StringTag
import top.mcfpp.Project
import top.mcfpp.annotations.InsertCommand
import top.mcfpp.lang.*
import top.mcfpp.lang.type.MCFPPGenericClassType
import top.mcfpp.lang.type.MCFPPType
import top.mcfpp.lib.*
import top.mcfpp.lib.function.Function
import top.mcfpp.lib.field.GlobalField
import top.mcfpp.lib.field.NamespaceField
import top.mcfpp.lib.function.FunctionParam
import top.mcfpp.lib.function.NoStackFunction
import top.mcfpp.lib.function.UnknownFunction
import top.mcfpp.lib.generic.Generic
import top.mcfpp.lib.generic.GenericClass
import top.mcfpp.util.LogProcessor
import top.mcfpp.util.StringHelper
import top.mcfpp.util.Utils
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.system.exitProcess

/**
 * 获取表达式结果用的visitor。解析并计算一个形如a+b*c的表达式。
 */
class McfppExprVisitor(private var defaultGenericClassType : MCFPPGenericClassType? = null): mcfppParserBaseVisitor<Var<*>?>() {

    private val tempVarCommandCache = HashMap<Var<*>, String>()

    var processVarCache : ArrayList<Var<*>> = ArrayList()
    fun clearCache(){ processVarCache.clear() }

    private var currSelector : CanSelectMember? = null



    /**
     * 计算一个复杂表达式
     * @param ctx the parse tree
     * @return 表达式的结果
     */
    @Override
    override fun visitExpression(ctx: mcfppParser.ExpressionContext): Var<*>? {
        Project.ctx = ctx
        val l = Function.currFunction
        val f = NoStackFunction("expression_${UUID.randomUUID()}",Function.currFunction)
        Function.currFunction = f
        return if(ctx.primary() != null){
            val q = visit(ctx.primary())
            Function.currFunction = l
            l.commands.addAll(f.commands)
            q
        }else{
            val q = visit(ctx.conditionalOrExpression())
            Function.currFunction = l
            if(q !is ReturnedMCBool){
                l.commands.addAll(f.commands)
            }else{
                //注册函数
                if(!GlobalField.localNamespaces.containsKey(f.namespace))
                    GlobalField.localNamespaces[f.namespace] = NamespaceField()
                GlobalField.localNamespaces[f.namespace]!!.addFunction(f,false)
            }
            q
        }
    }

    /*TODO 三目表达式。可能会实现，但是泠雪是懒狐，不想做。
     *@Override
     *public Var visitConditionalExpression(mcfppParser.ConditionalExpressionContext ctx){
     *    if(ctx.expression().size() == 0){
     *        return visit(ctx.conditionalOrExpression());
     *    }else {
     *        return null;
     *    }
     *}
    */

    /**
     * 计算一个或表达式。例如 a || b。
     * @param ctx the parse tree
     * @return 表达式的值
     */
    @Override
    override fun visitConditionalOrExpression(ctx: mcfppParser.ConditionalOrExpressionContext): Var<*>? {
        Project.ctx = ctx
        if(ctx.conditionalAndExpression().size != 1){
            val list = ArrayList<ReturnedMCBool>()
            val l = Function.currFunction
            var isConcrete = true
            var result = false
            for (i in 0 until ctx.conditionalAndExpression().size) {
                val temp = NoStackFunction("bool_${UUID.randomUUID()}",Function.currFunction)
                //注册函数
                if(!GlobalField.localNamespaces.containsKey(temp.namespace))
                    GlobalField.localNamespaces[temp.namespace] = NamespaceField()
                GlobalField.localNamespaces[temp.namespace]!!.addFunction(temp,false)
                Function.currFunction = temp
                val b: Var<*>? = visit(ctx.conditionalAndExpression(i))
                Function.currFunction = l
                if (b !is MCBool) {
                    LogProcessor.error("The operator \"&&\" cannot be used with ${b!!.type}")
                    throw IllegalArgumentException("")
                }
                if(b.isConcrete && isConcrete){
                    result = result || b.javaValue==true
                }else{
                    isConcrete = false
                }
            }
            if(isConcrete){
                return MCBool(result)
            }
            for (v in list){
                Function.addCommand("execute if function ${v.parentFunction.namespaceID} run return 1")
            }
            Function.addCommand("return 0")
            return ReturnedMCBool(Function.currFunction)
        }else{
            return visit(ctx.conditionalAndExpression(0))
        }
    }

    /**
     * 计算一个与表达式。例如a && b
     * @param ctx the parse tree
     * @return 表达式的值
     */
    //和
    @Override
    override fun visitConditionalAndExpression(ctx: mcfppParser.ConditionalAndExpressionContext): Var<*>? {
        Project.ctx = ctx
        if(ctx.equalityExpression().size != 1){
            val list = ArrayList<ReturnedMCBool>()
            val l = Function.currFunction
            var isConcrete = true
            var result = true
            for (i in 0 until ctx.equalityExpression().size) {
                val temp = NoStackFunction("bool_${UUID.randomUUID()}",Function.currFunction)
                //注册函数
                if(!GlobalField.localNamespaces.containsKey(temp.namespace))
                    GlobalField.localNamespaces[temp.namespace] = NamespaceField()
                GlobalField.localNamespaces[temp.namespace]!!.addFunction(temp,false)
                Function.currFunction = temp
                val b: Var<*>? = visit(ctx.equalityExpression(i))
                Function.currFunction = l
                if (b !is MCBool) {
                    LogProcessor.error("The operator \"&&\" cannot be used with ${b!!.type}")
                    throw IllegalArgumentException("")
                }
                if(b.isConcrete && isConcrete){
                    result = result && b.javaValue==true
                }else{
                    isConcrete = false
                }
            }
            if(isConcrete){
                return MCBool(result)
            }
            val sb = StringBuilder("execute ")
            for (v in list){
                sb.append("if function ${v.parentFunction.namespaceID} ")
            }
            sb.append("run return 1")
            Function.addCommand(sb.toString())
            Function.addCommand("return 0")
            return ReturnedMCBool(Function.currFunction)
        }else{
            return visit(ctx.equalityExpression(0))
        }
    }

    /**
     * 计算一个等于或不等于表达式，例如a == b和a != b
     * @param ctx the parse tree
     * @return 表达式的值
     */
    @Override
    override fun visitEqualityExpression(ctx: mcfppParser.EqualityExpressionContext): Var<*>? {
        Project.ctx = ctx
        var re: Var<*>? = visit(ctx.relationalExpression(0))
        if (ctx.relationalExpression().size != 1) {
            val b: Var<*>? = visit(ctx.relationalExpression(1))
            if(!re!!.isTemp){
                re = re.getTempVar()
            }
            if (ctx.op.text.equals("==")) {
                re = if (re is MCInt && b is MCInt) {
                    re.isEqual(b)
                } else if (re is MCBool && b is MCBool) {
                    re.equalCommand(b)
                } else if (re is MCFloat && b is MCFloat){
                    re.isEqual(b)
                } else{
                    LogProcessor.error("The operator \"${ctx.op.text}\" cannot be used between ${re.type} and ${b!!.type}")
                    throw IllegalArgumentException("")
                }
            } else {
                re = if (re is MCInt && b is MCInt) {
                    re.notEqual(b)
                } else if (re is MCBool && b is MCBool) {
                    re.notEqualCommand(b)
                } else if (re is MCFloat && b is MCFloat){
                    re.notEqual(b)
                }else{
                    LogProcessor.error("The operator \"${ctx.op.text}\" cannot be used between ${re.type} and ${b!!.type}")
                    throw IllegalArgumentException("")
                }
            }
        }
        return re
    }

    /**
     * 计算一个比较表达式，例如a > b
     * @param ctx the parse tree
     * @return 表达式的值
     */
    @Override
    override fun visitRelationalExpression(ctx: mcfppParser.RelationalExpressionContext): Var<*>? {
        Project.ctx = ctx
        var re: Var<*>? = visit(ctx.additiveExpression(0))
        if (ctx.additiveExpression().size != 1) {
            val b: Var<*>? = visit(ctx.additiveExpression(1))
            if (re is MCInt && b is MCInt) {
                when (ctx.relationalOp().text) {
                    ">" -> re = re.isGreater(b)
                    ">=" -> re = re.isGreaterOrEqual(b)
                    "<" -> re = re.isLess(b)
                    "<=" -> re = re.isLessOrEqual(b)
                }
            } else if(re is MCFloat && b is MCFloat){
                when (ctx.relationalOp().text) {
                    ">" -> re = re.isGreater(b)
                    ">=" -> re = re.isGreaterOrEqual(b)
                    "<" -> re = re.isLess(b)
                    "<=" -> re = re.isLessOrEqual(b)
                }
            }else {
                LogProcessor.error("The operator \"${ctx.relationalOp()}\" cannot be used between ${re!!.type} and ${b!!.type}")
                throw IllegalArgumentException("")
            }
        }
        return re
    }

    private var visitAdditiveExpressionRe : Var<*>? = null
    /**
     * 计算一个加减法表达式，例如a + b
     * @param ctx the parse tree
     * @return 表达式的值
     */
    @Override
    override fun visitAdditiveExpression(ctx: mcfppParser.AdditiveExpressionContext): Var<*>? {
        Project.ctx = ctx
        visitAdditiveExpressionRe = visit(ctx.multiplicativeExpression(0))
        processVarCache.add(visitAdditiveExpressionRe!!)
        for (i in 1 until ctx.multiplicativeExpression().size) {
            var b: Var<*>? = visit(ctx.multiplicativeExpression(i))
            if(b is MCFloat) b = b.toTempEntity()
            if(visitAdditiveExpressionRe!! != MCFloat.ssObj){
                visitAdditiveExpressionRe = visitAdditiveExpressionRe!!.getTempVar()
            }
            visitAdditiveExpressionRe = if (Objects.equals(ctx.op.text, "+")) {
                visitAdditiveExpressionRe!!.plus(b!!)
            } else if (Objects.equals(ctx.op.text, "-")) {
                visitAdditiveExpressionRe!!.minus(b!!)
            } else {
                null
            }
            if(visitAdditiveExpressionRe == null){
                LogProcessor.error("The operator \"${ctx.op.text}\" cannot be used between ${visitAdditiveExpressionRe!!.type} and ${b!!.type}.")
                Utils.stopCompile(IllegalArgumentException(""))
                exitProcess(1)
            }
            processVarCache[processVarCache.size - 1] = visitAdditiveExpressionRe!!
        }
        processVarCache.remove(visitAdditiveExpressionRe!!)
        return visitAdditiveExpressionRe
    }

    private var visitMultiplicativeExpressionRe : Var<*>? = null

    /**
     * 计算一个乘除法表达式，例如a * b
     * @param ctx the parse tree
     * @return 表达式的值
     */
    //乘法
    @Override
    override fun visitMultiplicativeExpression(ctx: mcfppParser.MultiplicativeExpressionContext): Var<*>? {
        Project.ctx = ctx
        visitMultiplicativeExpressionRe = visit(ctx.unaryExpression(0))
        processVarCache.add(visitMultiplicativeExpressionRe!!)
        for (i in 1 until ctx.unaryExpression().size) {
            var b: Var<*>? = visit(ctx.unaryExpression(i))
            if(b is MCFloat) b = b.toTempEntity()
            if(visitMultiplicativeExpressionRe != MCFloat.ssObj){
                visitMultiplicativeExpressionRe = visitMultiplicativeExpressionRe!!.getTempVar()
            }
            visitMultiplicativeExpressionRe = when(ctx.op.text){
                "*" -> {
                    visitAdditiveExpressionRe!!.multiple(b!!)
                }
                "/" -> {
                    visitAdditiveExpressionRe!!.divide(b!!)
                }
                "%" -> {
                    visitAdditiveExpressionRe!!.modular(b!!)
                }
                else -> null
            }
            if(visitMultiplicativeExpressionRe == null){
                LogProcessor.error("The operator \"${ctx.op.text}\" cannot be used between ${visitMultiplicativeExpressionRe!!.type} and ${b!!.type}.")
                Utils.stopCompile(IllegalArgumentException(""))
                exitProcess(1)
            }
            processVarCache[processVarCache.size - 1] = visitMultiplicativeExpressionRe!!
        }
        processVarCache.remove(visitMultiplicativeExpressionRe!!)
        return visitMultiplicativeExpressionRe
    }

    /**
     * 计算一个单目表达式。比如!a 或者 (int)a
     * @param ctx the parse tree
     * @return 表达式的值
     */
    @Override
    override fun visitUnaryExpression(ctx: mcfppParser.UnaryExpressionContext): Var<*>? {
        Project.ctx = ctx
        return if (ctx.rightVarExpression() != null) {
            visit(ctx.rightVarExpression())
        } else if (ctx.unaryExpression() != null) {
            var a: Var<*>? = visit(ctx.unaryExpression())
            if (a is MCBool) {
                if(!a.isTemp){
                    a = a.getTempVar()
                }
                a.negation()
            } else {
                LogProcessor.error("The operator \"!\" cannot be used with ${a!!.type}")
                Utils.stopCompile(IllegalArgumentException(""))
                exitProcess(1)
            }
        } else {
            //类型强制转换
            visit(ctx.castExpression())
        }
    }


    /**
     * 计算一个强制转换表达式。
     * @param ctx the parse tree
     * @return 表达式的值
     */
    @Override
    override fun visitCastExpression(ctx: mcfppParser.CastExpressionContext): Var<*> {
        Project.ctx = ctx
        val a: Var<*>? = visit(ctx.unaryExpression())
        return a!!.cast(MCFPPType.parseFromIdentifier(ctx.type().text, Function.currFunction.field))
    }

    /**
     * 对获取到的变量进行包装处理
     *
     * @param ctx
     * @return
     */
    @Override
    override fun visitRightVarExpression(ctx: mcfppParser.RightVarExpressionContext?): Var<*> {
        return visit(ctx!!.basicExpression())!!
        //return visit(ctx!!.basicExpression())!!.getTempVar(tempVarCommandCache)
    }

    /**
     * 计算一个基本的表达式。可能是一个变量，也可能是一个数值
     * @param ctx the parse tree
     * @return 表达式的值
     */
    @Override
    override fun visitBasicExpression(ctx: mcfppParser.BasicExpressionContext): Var<*>? {
        Project.ctx = ctx
        return if (ctx.primary() != null) {
            visit(ctx.primary())
        } else {
            visit(ctx.varWithSelector())
        }
    }

    /**
     * 从类中选择一个成员。返回的成员包含了它所在的对象的信息
     *
     * @param ctx
     * @return
     */
    @Override
    override fun visitVarWithSelector(ctx: mcfppParser.VarWithSelectorContext): Var<*>? {
        Project.ctx = ctx
        currSelector = if(ctx.primary() != null){
            //从变量中选择（非静态成员）
            visit(ctx.primary())
        }else{
            //从类型中选择（静态成员）
            //TODO 此处的词法需要更改，className和Identifier有冲突
            if(ctx.type().className() != null){
                //ClassName
                val clsstr = ctx.type().text.split(":")
                val qwq: Class? = if(clsstr.size == 2) {
                    GlobalField.getClass(clsstr[0], clsstr[1])
                }else{
                    GlobalField.getClass(null, clsstr[0])
                }
                if (qwq == null) {
                    LogProcessor.error("Undefined class:" + ctx.type().className().text)
                    return UnknownVar("${ctx.type().className().text}_type_" + UUID.randomUUID())
                }
                qwq.getType()
            }else{
                CompoundDataType(
                    //基本类型
                    when(ctx.type().text){
                        "int" -> MCInt.data
                        else -> TODO()
                    }
                )
            }

        }
        for (selector in ctx.selector()){
            visit(selector)
        }
        return currSelector as? Var<*>
    }

    @Override
    override fun visitSelector(ctx: mcfppParser.SelectorContext?): Var<*>? {
        //进入visitVar，currSelector作为成员选择的上下文
        currSelector = visit(ctx!!.`var`())!!.getTempVar()
        return null
    }

    /**
     * 一个初级表达式，可能是一个变量，也可能是一个数值
     * @param ctx the parse tree
     * @return 表达式的值
     */
    @Override
    override fun visitPrimary(ctx: mcfppParser.PrimaryContext): Var<*>? {
        Project.ctx = ctx
        if (ctx.`var`() != null) {
            //变量
            return visit(ctx.`var`())
        } else if (ctx.value() != null) {
            return visit(ctx.value())
        } else {
            //this或者super
            val re: Var<*>? = Function.field.getVar(ctx.text)
            if (re == null) {
                LogProcessor.error("${ctx.text} can only be used in member functions.")
            }
            return re
        }
    }

    /**
     * 变量
     * @param ctx the parse tree
     * @return 变量
     */
    @Override
    @InsertCommand
    override fun visitVar(ctx: mcfppParser.VarContext): Var<*>? {
        Project.ctx = ctx
        if (ctx.Identifier() != null && ctx.arguments() == null) {
            //变量
            //没有数组选取
            val qwq: String = ctx.Identifier().text
            var re = if(currSelector == null){
                val re: Var<*>? = Function.currFunction.field.getVar(qwq)
                if (re == null) {
                    LogProcessor.error("Undefined variable:$qwq")
                    throw Exception()
                }
                re
            }else{
                //获取成员
                val re  = currSelector!!.getMemberVar(qwq, currSelector!!.getAccess(Function.currFunction))
                if (re.first == null) {
                    LogProcessor.error("Undefined field: $qwq")
                }
                if (!re.second){
                    LogProcessor.error("Cannot access member $qwq")
                }
                re.first
            }
            // Identifier identifierSuffix*
            if (ctx.identifierSuffix() == null || ctx.identifierSuffix().size == 0) {
                return re
            } else {
                if(re is Indexable<*>){
                    for (value in ctx.identifierSuffix()) {
                        val index = visit(value.conditionalExpression())!!
                        re = (re as Indexable<*>).getByIndex(index)
                    }
                }else{
                    throw IllegalArgumentException("Cannot index ${re!!.type}")
                }
                return re
            }
        } else if (ctx.expression() != null) {
            // '(' expression ')'
            return McfppExprVisitor().visit(ctx.expression())
        } else {
            //是函数调用，将已经计算好的中间量存储到栈中
            for (v in processVarCache){
                v.storeToStack()
            }
            //函数的调用
            Function.addCommand("#" + ctx.text)
            //参数获取
            val normalArgs: ArrayList<Var<*>> = ArrayList()
            val readOnlyArgs: ArrayList<Var<*>> = ArrayList()
            val exprVisitor = McfppExprVisitor()
            for (expr in ctx.arguments().readOnlyArgs()?.expressionList()?.expression()?: emptyList()) {
                val arg = exprVisitor.visit(expr)!!
                readOnlyArgs.add(arg)
            }
            for (expr in ctx.arguments().normalArgs().expressionList()?.expression()?: emptyList()) {
                val arg = exprVisitor.visit(expr)!!
                normalArgs.add(arg)
            }
            //获取函数
            val p = StringHelper.splitNamespaceID(ctx.namespaceID().text)
            val func = if(currSelector == null){
                GlobalField.getFunction(p.first, p.second, FunctionParam.getArgTypeNames(readOnlyArgs), FunctionParam.getArgTypeNames(normalArgs))
            }else{
                if(p.first != null){
                    LogProcessor.warn("Invalid namespace usage ${p.first} in function call ")
                }
                McfppFuncManager().getFunction(currSelector!!,p.second,
                    FunctionParam.getArgTypes(readOnlyArgs),
                    FunctionParam.getArgTypes(normalArgs))
            }
            //调用函数
            return if (func is UnknownFunction) {
                //可能是构造函数
                var cls: Class? = GlobalField.getClass(p.first, p.second)
                if (cls == null) {
                    LogProcessor.error("Function " + ctx.text + " not defined")
                    Function.addCommand("[Failed to Compile]${ctx.text}")
                    func.invoke(normalArgs,currSelector)
                    return func.returnVar
                }
                if(cls is GenericClass){
                    if(defaultGenericClassType != null){
                        //比对实例化参数
                        //参数不一致
                        if(defaultGenericClassType!!.genericVar.size != readOnlyArgs.size){
                            LogProcessor.error("Generic class ${cls.identifier} requires ${cls.readOnlyParams.size} type arguments, but ${readOnlyArgs.size} were provided")
                            return UnknownVar("${cls.identifier}_type_" + UUID.randomUUID())
                        }
                        //参数缺省
                        if(readOnlyArgs.isEmpty()){
                            readOnlyArgs.addAll(defaultGenericClassType!!.genericVar)
                        }
                    }
                    //实例化泛型函数
                    cls = cls.compile(readOnlyArgs)
                }
                //获取对象
                val ptr = cls.newInstance()
                //调用构造函数
                val constructor = cls.getConstructor(FunctionParam.getArgTypeNames(normalArgs))
                if (constructor == null) {
                    LogProcessor.error("No constructor like: " + FunctionParam.getArgTypeNames(normalArgs) + " defined in class " + ctx.namespaceID().text)
                    Function.addCommand("[Failed to compile]${ctx.text}")
                }else{
                    constructor.invoke(normalArgs, callerClassP = ptr)
                }
                return ptr
            }else{
                if(func is Generic<*>){
                    func.invoke(readOnlyArgs, normalArgs, currSelector)
                }else{
                    func.invoke(normalArgs,currSelector)
                }
                for (v in processVarCache){
                    v.getFromStack()
                }
                //函数树
                Function.currFunction.child.add(func)
                func.parent.add(Function.currFunction)
                func.returnVar
            }
        }
    }

    override fun visitValue(ctx: mcfppParser.ValueContext): Var<*>? {
        //常量
        if (ctx.intValue() != null) {
            return MCInt(Integer.parseInt(ctx.intValue().text))
        } else if (ctx.LineString() != null) {
            val r: String = ctx.LineString().text
            return MCString(StringTag(r.substring(1, r.length - 1)))
        } else if (ctx.multiLineStringLiteral()!=null){
            val stringArray = mutableListOf<String>()
            var isConcrete = true
            for(stringContext in ctx.multiLineStringLiteral().multiLineStringContent()){
                var r:String
                if(stringContext.MultiLineStrText()!=null) r= stringContext.MultiLineStrText().text
                else if(stringContext.MultiLineStringQuote()!=null) r= stringContext.MultiLineStringQuote().text
                else {
                    val expressionContext = stringContext.multiLineStringExpression().expression()
                    //TODO: 这边只是简单写了一下有解析值的情况
                    val res = visit(expressionContext) //没有解析值的话，应该变成jtext
                    if(res!=null && !res.isConcrete){ isConcrete = false } //这个条件就是说，整个模版中出现没有解析值的情况了
                    if(res is MCInt){
                        r = res.javaValue.toString()
                    }
                    else{
                        r=res.toString()
                    }
                }
                stringArray.add(r);
            }
            val tailQuote = ctx.multiLineStringLiteral().TRIPLE_QUOTE_CLOSE().text
            if(tailQuote.length>3) {
                stringArray.add(tailQuote.substring(3,tailQuote.length))
            }
            return MCString(StringTag(stringArray.joinToString("")) ) //没有解析值就变不了MCString了
        } else if (ctx.floatValue() != null){
            return MCFloat(ctx.floatValue()!!.text.toFloat())
        } else if (ctx.boolValue() != null){
            return MCBool(ctx.boolValue()!!.text.toBoolean())
        } else if (ctx.nbtValue() != null){
            return NBTBasedData(SNBTUtil.fromSNBT(ctx.nbtValue().text))
        } else if (ctx.type() != null){
            return MCFPPTypeVar(MCFPPType.parseFromIdentifier(ctx.type().text, Function.currFunction.field))
        }
        return null
    }

}