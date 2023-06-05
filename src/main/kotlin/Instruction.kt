class Instruction {
    var locationCounter: String? = null
    var objectCode: String? = null
    var isExtended = false
    var isIndirect = false
    var isImmediate = false
    var isIndexed = false
    var isBaseRelative = false
    var isPCRelative = false
    var label: String? = null
    var opcode: String = ""
    var operand: String = ""

    constructor(label: String?, opcode: String, operand: String) {
        this.label = label
        this.opcode = opcode
        this.operand = operand

        if (this.label == ".") {
            return
        }

        if (this.opcode[0] == '+') {
            isExtended = true
            this.opcode = opcode.substring(1)
        }

        if (this.opcode == "RSUB") {
            return
        }

        if (this.operand[0] == '@') {
            this.operand = operand.substring(1)
            isIndirect = true
            isImmediate = false
        } else if(this.operand[0] == '#') {
            this.operand = operand.substring(1)
            isIndirect = false
            isImmediate = true
        } else if(this.operand.endsWith(",X") && getOpcodeType() == 3) {
            isIndirect = false
            isImmediate = false
            isIndexed = true
        } else {
            isIndirect = false
            isImmediate = false
        }
    }

    fun getNIXBPE(): String {
        val n = if (isIndirect) "1" else "0"
        val i = if (isImmediate) "1" else "0"
        val x = if (isIndexed) "1" else "0"
        val b = if (isBaseRelative) "1" else "0"
        val p = if (isPCRelative) "1" else "0"
        val e = if (isExtended) "1" else "0"
        return n + i + x + b + p + e
    }

    fun getOpcodeType(): Int {
        if (label == ".") {
            return 0
        }
        return if (isExtended) {
            4
        } else when (opcode) {
            "ADD", "DIV", "MUL", "COMP", "J", "JEQ", "JGT", "JLT", "JSUB", "LDA", "LDB", "LDCH", "LDL", "LDT", "LDX", "RD", "RSUB", "STA", "STB", "STCH", "STL", "STT", "STX", "SUB", "TD", "TIX", "WD" -> 3
            "CLEAR", "COMPR", "TIXR" -> 2
            else -> 0
        }
    }

    fun getByteObjectCode(): String {
        return if (operand.startsWith("C")) { //C'EOF'
            val operandValue = operand.substring(2, operand.length - 1)
            var hexValue = ""
            for (element in operandValue) {
                hexValue += Integer.toHexString(element.code)
            }
            hexValue
        } else { //X'F1'
            operand.substring(2, operand.length - 1)
        }
    }

    override fun toString(): String {
        if (objectCode == null) {
            return "comment"
        }
        var locString = String.format("%4s", locationCounter).replace(' ', '0')
        val objcodeString = String.format("%-12s", objectCode!!.uppercase())

        if (objectCode!!.uppercase() == "NONE") {
            return String.format("%-8s", locString) + "NONE"
        }

        locString = String.format("%-8s", locString)
        val nixbpeString = "nixbpe:" + getNIXBPE()
        val instructionFormatTypeAndAddressingMode = if (isExtended) {
            String.format("%-17s", "format 4")
        } else if(getOpcodeType() == 3) {
            if (isPCRelative) {
                String.format("%-17s", "pc-relative")
            } else if(isBaseRelative) {
                String.format("%-17s", "base-relative")
            } else {
                String.format("%-17s", "absolute")
            }
        } else if(getOpcodeType() == 2) {
            String.format("%-17s", "format 2")
        } else {
            String.format("%-17s", "simple")
        }

        return locString + objcodeString + instructionFormatTypeAndAddressingMode + nixbpeString
    }
}