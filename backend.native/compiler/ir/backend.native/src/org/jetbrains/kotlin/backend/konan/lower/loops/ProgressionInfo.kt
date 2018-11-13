package org.jetbrains.kotlin.backend.konan.lower.loops

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.irasdescriptors.isSubtypeOf
import org.jetbrains.kotlin.backend.konan.lower.matchers.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

enum class ProgressionType(val numberCastFunctionName: Name) {
    INT_PROGRESSION(Name.identifier("toInt")),
    LONG_PROGRESSION(Name.identifier("toLong")),
    CHAR_PROGRESSION(Name.identifier("toChar"));
}

internal data class ProgressionInfo(
        val progressionType: ProgressionType,
        val first: IrExpression,
        val bound: IrExpression,
        val step: IrExpression? = null,
        val increasing: Boolean = true,
        var needLastCalculation: Boolean = false,
        val closed: Boolean = true,
        val collectionReference: IrValueDeclaration? = null)

internal interface ProgressionHandler {
    val matcher: IrCallMatcher

    fun build(call: IrCall, progressionType: ProgressionType): ProgressionInfo?

    fun handle(irCall: IrCall, progressionType: ProgressionType) = if (matcher(irCall)) {
        build(irCall, progressionType)
    } else {
        null
    }
}

internal class ProgressionInfoBuilder(val context: Context) : IrElementVisitor<ProgressionInfo?, Nothing?> {

    private val symbols = context.ir.symbols

    private val progressionElementClasses = symbols.integerClasses + symbols.char

    private val progressionHandlers = listOf(
            IndicesHandler(context),
            UntilHandler(progressionElementClasses),
            DownToHandler(progressionElementClasses),
            StepHandler(context, this),
            RangeToHandler(progressionElementClasses)
    )

    private val collectionIterationHandler = CollectionIterationHandler(context)

    private fun IrType.getProgressionType(): ProgressionType? = when {
        isSubtypeOf(symbols.charProgression.owner.defaultType) -> ProgressionType.CHAR_PROGRESSION
        isSubtypeOf(symbols.intProgression.owner.defaultType) -> ProgressionType.INT_PROGRESSION
        isSubtypeOf(symbols.longProgression.owner.defaultType) -> ProgressionType.LONG_PROGRESSION
        else -> null
    }
    override fun visitElement(element: IrElement, data: Nothing?): ProgressionInfo? = null

    override fun visitVariable(declaration: IrVariable, data: Nothing?): ProgressionInfo? {
        return declaration.initializer?.accept(this, null)
    }

    override fun visitCall(expression: IrCall, data: Nothing?): ProgressionInfo? {
        val progressionType = expression.type.getProgressionType()
                // Try to handle call with collection handler
                ?: return collectionIterationHandler.handle(expression, ProgressionType.INT_PROGRESSION)
                        ?: return expression.dispatchReceiver?.accept(this, null)

        return progressionHandlers.firstNotNullResult { it.handle(expression, progressionType) }
    }
}
