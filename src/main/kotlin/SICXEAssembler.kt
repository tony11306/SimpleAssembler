import java.io.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.system.exitProcess

class SICXEAssembler {
    private var instructions = ArrayList<Instruction>()
    private val registerTable = hashMapOf(
        "A" to "0",
        "X" to "1",
        "L" to "2",
        "B" to "3",
        "S" to "4",
        "T" to "5",
        "F" to "6",
        "PC" to "8",
        "SW" to "9"
    )
    private var symbolTable = HashMap<String?, String?>()
    private val opcodeTable = hashMapOf(
        "ADD" to "18",
        "CLEAR" to "B4",
        "COMP" to "28",
        "COMPR" to "A0",
        "DIV" to "24",
        "J" to "3C",
        "JEQ" to "30",
        "JGT" to "34",
        "JLT" to "38",
        "JSUB" to "48",
        "LDA" to "00",
        "LDB" to "68",
        "LDCH" to "50",
        "LDL" to "08",
        "LDT" to "74",
        "LDX" to "04",
        "MUL" to "20",
        "RD" to "D8",
        "RSUB" to "4C",
        "STA" to "0C",
        "STB" to "78",
        "STCH" to "54",
        "STL" to "14",
        "STT" to "84",
        "STX" to "10",
        "SUB" to "1C",
        "TD" to "E0",
        "TIX" to "2C",
        "TIXR" to "B8",
        "WD" to "DC",
        "START" to "",
        "END" to "",
        "BASE" to "",
        "RESW" to "",
        "RESB" to "",
        "WORD" to "",
        "BYTE" to "",
        "EQU" to "",
    )
    private var base = ""

    fun convertToObjectCode(inputFilePath: String, outputFilePath: String) {
        this.readInput(inputFilePath) // 讀取輸入文件
        this.validateInstructionsOpcode() // 初步判斷指令是否存在
        this.calculateLocationCounter() // 計算 location counter
        this.initSymbolTable() // 初始化symbol table
        this.generateObjectCode() // 生成 object code
        val result = this.getAssemblerResult() // 獲取組譯結果
        this.exportFile(outputFilePath, result) // 輸出組譯結果
    }

    private fun isNumeric(str: String): Boolean {
        return str.matches("\\d+".toRegex())
    }

    private fun readInput(filePath: String) {
        try {
            val reader = BufferedReader(FileReader(filePath))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!![0] == '.') {
                    instructions.add(Instruction(".", "", ""))
                    continue
                }

                if (line!!.length < 25) {
                    line = String.format("%-25s", line) // 用空格補齊至25个字符
                }
                val label = line!!.substring(0, 9).trim { it <= ' ' } // 第一部分
                val opcode = line!!.substring(12, 21).trim { it <= ' ' } // 第二部分
                val operand = line!!.substring(24).trim { it <= ' ' } // 第三部分
                instructions.add(Instruction(label, opcode, operand))
            }
            reader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun validateInstructionsOpcode() { //檢查指令是否存在於OPTAB中
        for (instruction in instructions) {
            if (instruction.label == ".") {
                continue
            }
            if (opcodeTable.containsKey(instruction.opcode)) {
                continue
            } else {
                println("Error: " + instruction.opcode + " is not a valid opcode.")
                exitProcess(0)
            }
        }
    }

    private fun calculateLocationCounter() {
        var locationCounter = 0
        val instructionLocationCounterMap = hashMapOf(
            "" to 0,
            "." to 0,
            "END" to 0,
            "WORD" to 3,
            "EQU" to 0,
            "ADD" to 3,
            "CLEAR" to 2,
            "COMP" to 3,
            "COMPR" to 2,
            "DIV" to 3,
            "J" to 3,
            "JEQ" to 3,
            "JGT" to 3,
            "JLT" to 3,
            "JSUB" to 3,
            "LDA" to 3,
            "LDB" to 3,
            "LDCH" to 3,
            "LDL" to 3,
            "LDT" to 3,
            "LDX" to 3,
            "MUL" to 3,
            "RD" to 3,
            "RSUB" to 3,
            "STA" to 3,
            "STB" to 3,
            "STCH" to 3,
            "STL" to 3,
            "STT" to 3,
            "STX" to 3,
            "SUB" to 3,
            "TD" to 3,
            "TIX" to 3,
            "TIXR" to 2,
            "WD" to 3,
        )
        for (instruction in instructions) {
            //計算location counter
            if (instruction.isExtended) {
                instruction.locationCounter = Integer.toHexString(locationCounter)
                locationCounter += 4
            } else if (instruction.opcode in instructionLocationCounterMap) {
                instruction.locationCounter = Integer.toHexString(locationCounter)
                locationCounter += instructionLocationCounterMap[instruction.opcode]!!
            } else if (instruction.opcode == "START") {
                locationCounter = instruction.operand.toInt()
                instruction.locationCounter = Integer.toHexString(instruction.operand.toInt())
            } else if (instruction.opcode == "BASE") {
                base = instruction.operand
                instruction.locationCounter = Integer.toHexString(locationCounter)
            } else if (instruction.opcode == "RESW") {
                instruction.locationCounter = Integer.toHexString(locationCounter)
                locationCounter += 3 * instruction.operand.toInt()
            } else if (instruction.opcode == "RESB") {
                instruction.locationCounter = Integer.toHexString(locationCounter)
                locationCounter += instruction.operand.toInt()
            } else if (instruction.opcode == "BYTE") {
                instruction.locationCounter = Integer.toHexString(locationCounter)
                if (instruction.operand[0] == 'C') {
                    locationCounter += instruction.operand.length - 3
                } else if (instruction.operand[0] == 'X') {
                    locationCounter += (instruction.operand.length - 3) / 2
                }
            } else {
                println("Error: " + instruction.opcode + " is not a valid opcode.")
                exitProcess(0)
            }
        }
    }

    private fun initSymbolTable() { //初始化符號表
        for (instruction in instructions) {
            if (instruction.label === "." || instruction.label === "") continue
            if (instruction.label != null) {
                symbolTable[instruction.label] = instruction.locationCounter
            }
        }
    }

    private fun generateObjectCode() {
        for (instruction in instructions) {

            if (instruction.opcode === "" || instruction.opcode === ".") continue
            when (instruction.opcode) {
                "ADD", "DIV", "MUL", "COMP", "J", "JEQ", "JGT", "JLT", "JSUB", "LDA", "LDB", "LDCH", "LDL", "LDT", "LDX", "RD", "RSUB", "STA", "STB", "STCH", "STL", "STT", "STX", "SUB", "TD", "TIX", "WD" -> instruction.objectCode =
                    getType3ObjCode(instruction)
                "CLEAR", "COMPR", "TIXR" -> instruction.objectCode = getType2ObjCode(instruction)
                "BYTE" -> instruction.objectCode = instruction.getByteObjectCode()
                "WORD" -> instruction.objectCode = instruction.getByteObjectCode()
                "RESB", "RESW", "BASE", "START", "END" -> instruction.objectCode = "none"
                "EQU" -> {
                    handleEQU(instruction)
                    instruction.objectCode = "none"
                }
                else -> instruction.objectCode = "!"
            }
        }
    }

    private fun getType3ObjCode(instruction: Instruction): String {
        val opcode = instruction.opcode
        var operand =
            if (instruction.isIndexed) instruction.operand.split(",".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()[0].uppercase(Locale.getDefault())
                .trim { it <= ' ' } else instruction.operand.uppercase(Locale.getDefault())
        val pc = if (instruction.isExtended) Integer.toHexString(
            instruction.locationCounter!!.toInt(16) + 4
        ) else Integer.toHexString(instruction.locationCounter!!.toInt(16) + 3)
        var disp = ""
        if (operand.contains("+")) { //計算disp十六進位
            val operands =
                operand.split("\\+".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            val operand1 = operands[0]
            val operand2 = operands[1]
            //未定義的symbol
            if (!symbolTable.containsKey(operand1) && !isNumeric(operand1) || !symbolTable.containsKey(
                    operand2
                ) && !isNumeric(operand2)
            ) {
                println("Error: undefined symbol")
                println(instruction.opcode + " " + instruction.operand)
                exitProcess(0)
            }
            operand = if (symbolTable.containsKey(operand1) && symbolTable.containsKey(
                    operand2
                )
            ) { //symbol + symbol
                val operand1Value = symbolTable[operand1]!!.toInt(16)
                val operand2Value = symbolTable[operand2]!!.toInt(16)
                val operandValue = operand1Value + operand2Value
                Integer.toHexString(operandValue)
            } else if (symbolTable.containsKey(operand1)) { //symbol + constant
                val operand1Value = symbolTable[operand1]!!.toInt(16)
                val operand2Value = operand2.toInt(16)
                val operandValue = operand1Value + operand2Value
                Integer.toHexString(operandValue)
            } else if (symbolTable.containsKey(operand2)) { //constant + symbol
                val operand1Value = operand1.toInt(16)
                val operand2Value = symbolTable[operand2]!!.toInt(16)
                val operandValue = operand1Value + operand2Value
                Integer.toHexString(operandValue)
            } else { //constant + constant
                val operand1Value = operand1.toInt(16)
                val operand2Value = operand2.toInt(16)
                val operandValue = operand1Value + operand2Value
                Integer.toHexString(operandValue)
            }
        } else if (operand.contains("-")) { //計算disp十六進位
            val operands = operand.split("-")
            val operand1 = operands[0]
            val operand2 = operands[1]
            //未定義的symbol
            if (!symbolTable.containsKey(operand1) && !isNumeric(operand1) || !symbolTable.containsKey(
                    operand2
                ) && !isNumeric(operand2)
            ) {
                println("Error: undefined symbol")
                println(instruction.opcode + " " + instruction.operand)
                exitProcess(0)
            }
            operand = if (symbolTable.containsKey(operand1) && symbolTable.containsKey(
                    operand2
                )
            ) { //symbol - symbol
                val operand1Value = symbolTable[operand1]!!.toInt(16)
                val operand2Value = symbolTable[operand2]!!.toInt(16)
                val operandValue = operand1Value - operand2Value
                Integer.toHexString(operandValue)
            } else if (symbolTable.containsKey(operand1)) { //symbol - constant
                val operand1Value = symbolTable[operand1]!!.toInt(16)
                val operand2Value = operand2.toInt(16)
                val operandValue = operand1Value - operand2Value
                Integer.toHexString(operandValue)
            } else if (symbolTable.containsKey(operand2)) { //constant - symbol
                val operand1Value = operand1.toInt(16)
                val operand2Value = symbolTable[operand2]!!.toInt(16)
                val operandValue = operand1Value - operand2Value
                Integer.toHexString(operandValue)
            } else { //constant - constant
                val operand1Value = operand1.toInt(16)
                val operand2Value = operand2.toInt(16)
                val operandValue = operand1Value - operand2Value
                Integer.toHexString(operandValue)
            }
        }
        if (instruction.isExtended) { //extended
            val operand1 = operand
            if (!symbolTable.containsKey(operand1) && !isNumeric(operand1)) { //未定義的symbol
                println("Error: undefined symbol")
                println(instruction.opcode + " " + instruction.operand)
                exitProcess(0)
            }
            if (symbolTable.containsKey(operand)) {
                val operandValue = symbolTable[operand]!!.toInt(16)
                disp = Integer.toHexString(operandValue)
            } else if (isNumeric(operand)) {
                disp = operand
            }
        } else if (instruction.isImmediate) {
            val operand1 = operand
            if (!symbolTable.containsKey(operand1) && !isNumeric(operand1)) { //未定義的symbol
                println("Error: undefined symbol")
                println(instruction.opcode + " " + instruction.operand)
                exitProcess(0)
            }
            if (symbolTable.containsKey(operand) && !isNumeric(operand)) {
                val pcValue = pc.toInt(16)
                val operandValue = symbolTable[operand]!!.toInt(16)
                var operandDistance = operandValue - pcValue
                if (operandDistance >= -2048 && operandDistance <= 2047) { //pc relative
                    instruction.isPCRelative = true
                    instruction.isBaseRelative = false
                    disp = Integer.toHexString(operandDistance)
                } else { //base relative
                    instruction.isPCRelative = false
                    instruction.isBaseRelative = true
                    val baseValue =
                        symbolTable[base]!!
                            .toInt(16)
                    operandDistance = operandValue - baseValue
                    disp = Integer.toHexString(operandDistance)
                }
            } else if (isNumeric(operand)) {
                disp = operand
            }
        } else if (instruction.opcode == "RSUB") { //rsub
            return "4F0000"
        } else { //pc or base
            val operand1 = operand
            if (!symbolTable.containsKey(operand1) && !isNumeric(operand1)) { //未定義的symbol
                println("Error: undefined symbol")
                println(instruction.opcode + " " + instruction.operand)
                exitProcess(0)
            }
            if (symbolTable.containsKey(operand)) {
                //計算pc與operand的距離
                val pcValue = pc.toInt(16)
                val operandValue = symbolTable[operand]!!.toInt(16)
                var operandDistance = operandValue - pcValue
                if (operandDistance >= -2048 && operandDistance <= 2047) { //pc relative
                    instruction.isPCRelative = true
                    instruction.isBaseRelative = false
                    disp = Integer.toHexString(operandDistance)
                } else { //base relative
                    instruction.isPCRelative = false
                    instruction.isBaseRelative = true
                    val baseValue =
                        symbolTable[base]!!
                            .toInt(16)
                    operandDistance = operandValue - baseValue
                    disp = Integer.toHexString(operandDistance)
                }
            } else if (isNumeric(operand)) {
                val pcValue = pc.toInt(16)
                val operandValue = operand.toInt(16)
                var operandDistance = operandValue - pcValue
                if (operandDistance >= -2048 && operandDistance <= 2047) { //pc relative
                    instruction.isPCRelative = true
                    instruction.isBaseRelative = false
                    disp = Integer.toHexString(operandDistance)
                } else { //base relative
                    instruction.isPCRelative = false
                    instruction.isBaseRelative = true
                    val baseValue =
                        symbolTable[base]!!
                            .toInt(16)
                    operandDistance = operandValue - baseValue
                    disp = Integer.toHexString(operandDistance)
                }
            }
        }
        if (!instruction.isIndirect && !instruction.isImmediate) {
            instruction.isIndirect = true
            instruction.isImmediate = true
        }

        //轉為2進位
        var binaryOpcode = java.lang.Long.toBinaryString(
            opcodeTable[opcode]!!.toLong(16)
        )
        //補0至8位元
        while (binaryOpcode.length < 8) {
            binaryOpcode = "0$binaryOpcode"
        }
        //消除末兩位
        binaryOpcode = binaryOpcode.substring(0, binaryOpcode.length - 2)
        //disp
        if (!instruction.isExtended && disp.length > 3) {
            disp = disp.substring(disp.length - 3, disp.length)
        } else if (!instruction.isExtended && disp.length < 3) {
            while (disp.length < 3) {
                disp = "0$disp"
            }
        } else if (instruction.isExtended && disp.length < 5) {
            while (disp.length < 5) {
                disp = "0$disp"
            }
        }

        //16進位轉2進位
        disp = java.lang.Long.toBinaryString(disp.toLong(16))
        //如果是extend，補0至20位元
        if (instruction.isExtended) {
            while (disp.length < 20) {
                disp = "0$disp"
            }
        } else { //否則補0至12位元
            while (disp.length < 12) {
                disp = "0$disp"
            }
        }
        val temp = binaryOpcode + instruction.getNIXBPE() + disp

        //從左到右，每4位元換成16進位
        val sb = StringBuilder()
        var i = 0
        while (i < temp.length) {
            val str = temp.substring(i, i + 4)
            val decimal = str.toInt(2)
            sb.append(Integer.toHexString(decimal))
            i += 4
        }
        return sb.toString()
    }

    private fun getType2ObjCode(instruction: Instruction): String {
        return if (instruction.opcode == "COMPR") {
            val opcode = opcodeTable[instruction.opcode]
            val r1 = instruction.operand.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()[0]
            val r2 = instruction.operand.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()[1]
            //將16進位字串REGTAB.get(r1)改為2進位字串
            val r1Value = registerTable[r1]
            val r2Value = registerTable[r2]
            opcode + r1Value + r2Value
        } else {
            if (instruction.opcode == "CLEAR") {
                val opcode = opcodeTable[instruction.opcode]
                val r1 = instruction.operand
                val r1Value = registerTable[r1]
                opcode + r1Value + "0"
            } else { //TIXR
                val opcode = opcodeTable[instruction.opcode]
                val r1 = instruction.operand
                val r1Value = registerTable[r1]
                opcode + r1Value + "0"
            }
        }
    }

    private fun handleEQU(instruction: Instruction) {
        var operand: String? = instruction.operand
        if (operand!!.contains("+")) { //計算disp十六進位
            val operands = operand.split("\\+".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val operand1 = operands[0]
            val operand2 = operands[1]
            //未定義的symbol
            if (!symbolTable.containsKey(operand1) && !isNumeric(operand1) || !symbolTable.containsKey(
                    operand2
                ) && !isNumeric(operand2)
            ) {
                println("Error: undefined symbol")
                println("EQU")
                exitProcess(0)
            }
            operand =
                if (symbolTable.containsKey(operand1) && symbolTable.containsKey(operand2)) { //symbol + symbol
                    val operand1Value = symbolTable[operand1]!!.toInt(16)
                    val operand2Value = symbolTable[operand2]!!.toInt(16)
                    val operandValue = operand1Value + operand2Value
                    Integer.toHexString(operandValue)
                } else if (symbolTable.containsKey(operand1)) { //symbol + constant
                    val operand1Value = symbolTable[operand1]!!.toInt(16)
                    val operand2Value = operand2.toInt(16)
                    val operandValue = operand1Value + operand2Value
                    Integer.toHexString(operandValue)
                } else if (symbolTable.containsKey(operand2)) { //constant + symbol
                    val operand1Value = operand1.toInt(16)
                    val operand2Value = symbolTable[operand2]!!.toInt(16)
                    val operandValue = operand1Value + operand2Value
                    Integer.toHexString(operandValue)
                } else { //constant + constant
                    val operand1Value = operand1.toInt(16)
                    val operand2Value = operand2.toInt(16)
                    val operandValue = operand1Value + operand2Value
                    Integer.toHexString(operandValue)
                }
        } else if (operand.contains("-")) { //計算disp十六進位
            val operands = operand.split("-")
                .toTypedArray()
            val operand1 = operands[0]
            val operand2 = operands[1]
            //未定義的symbol
            if (!symbolTable.containsKey(operand1) && !isNumeric(operand1) || !symbolTable.containsKey(
                    operand2
                ) && !isNumeric(operand2)
            ) {
                println("Error: undefined symbol")
                println("EQU")
                exitProcess(0)
            }
            operand =
                if (symbolTable.containsKey(operand1) && symbolTable.containsKey(operand2)) { //symbol - symbol
                    val operand1Value = symbolTable[operand1]!!.toInt(16)
                    val operand2Value = symbolTable[operand2]!!.toInt(16)
                    val operandValue = operand1Value - operand2Value
                    Integer.toHexString(operandValue)
                } else if (symbolTable.containsKey(operand1)) { //symbol - constant
                    val operand1Value = symbolTable[operand1]!!.toInt(16)
                    val operand2Value = operand2.toInt(16)
                    val operandValue = operand1Value - operand2Value
                    Integer.toHexString(operandValue)
                } else if (symbolTable.containsKey(operand2)) { //constant - symbol
                    val operand1Value = operand1.toInt(16)
                    val operand2Value = symbolTable[operand2]!!.toInt(16)
                    val operandValue = operand1Value - operand2Value
                    Integer.toHexString(operandValue)
                } else { //constant - constant
                    val operand1Value = operand1.toInt(16)
                    val operand2Value = operand2.toInt(16)
                    val operandValue = operand1Value - operand2Value
                    Integer.toHexString(operandValue)
                }
        } else if (symbolTable.containsKey(operand)) {
            operand = symbolTable[operand]
        } else if (operand.contains("*")) {
            operand = instruction.locationCounter
        }
        symbolTable.replace(instruction.label, operand)
    }

    private fun exportFile(fileName: String, text: String) {
        try {
            val fileWriter = FileWriter(fileName)
            val writer = BufferedWriter(fileWriter)
            writer.write(text)
            writer.close()
        } catch (e: IOException) {
            println("!：" + e.message)
        }
    }

    private fun getAssemblerResult(): String {
        var output = "=SYMTAB=\n"
        //將hashmap轉為list
        val list: List<Map.Entry<String?, String?>> = ArrayList<Map.Entry<String?, String?>>(
            symbolTable.entries
        )
        Collections.sort(list, object : Comparator<Map.Entry<String?, String?>?> {
            //升序排序
            override fun compare(
                o1: Map.Entry<String?, String?>?,
                o2: Map.Entry<String?, String?>?
            ): Int {
                val v1 = o1!!.value?.toInt(16)
                val v2 = o2!!.value?.toInt(16)
                if (v1 != null && v2 != null) {
                    return if (v1 > v2) {
                        1
                    } else if (v1 < v2) {
                        -1
                    } else {
                        0
                    }
                }
                return 0
            }
        })

        for (line in list) {
            val value = String.format("%4s", line.value).replace(' ', '0')
                .uppercase(Locale.getDefault())
            val formattedKey = String.format("%-10s", line.key)
            val formattedValue = String.format("%-10s", value)
            output += "$formattedKey $formattedValue\n"
        }
        output += "\n=OPTAB=\n"

        for (instruction in instructions) {
            output += instruction.toString() + "\n"
        }

        return output
    }
}