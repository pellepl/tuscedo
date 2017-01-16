package com.pelleplutt.plang;

import java.util.HashMap;
import java.util.Map;

import com.pelleplutt.plang.ASTNode.ASTNodeBlok;
import com.pelleplutt.plang.Source.SourceString;
import com.pelleplutt.plang.proc.Assembler;
import com.pelleplutt.plang.proc.ExtCall;
import com.pelleplutt.plang.proc.Processor;
import com.pelleplutt.plang.proc.ProcessorError;
import com.pelleplutt.plang.proc.ProcessorError.ProcessorFinishedError;

public class Compiler {
  static String src;
  
  public static Executable compile(Map<String, ExtCall> extDefs, int ramOffs, int constOffs, String ...sources) {
    Source srcs[] = new Source[sources.length];
    int i = 0;
    for (String s : sources) {
      srcs[i++] = new Source.SourceString("<string>", s);
    }
    return compile(extDefs, ramOffs, constOffs, srcs);
  }

  public static Executable compile(Map<String, ExtCall> extDefs, int ramOffs, int constOffs, Source ...sources) {
    IntermediateRepresentation ir = null;
    for (Source osrc : sources) {
      String src = osrc.getSource();
      Compiler.src = src;
      //System.out.println("* 1. build tree");
      //AST.dbg = true;
      ASTNodeBlok e = AST.buildTree(src);
      //System.out.println(e);
      
      //System.out.println("* 2. optimise tree");
      ASTOptimiser.optimise(e);
      //System.out.println(e);
  
      //System.out.println("* 3. check grammar");
      //Grammar.dbg = true;
      Grammar.check(e);
  
      //System.out.println("* 4. structural analysis");
      //StructAnalysis.dbg = true;
      StructAnalysis.analyse(e, ir);
      
      //System.out.println("* 5. intermediate codegen");
      //CodeGenFront.dbg = true;
      //CodeGenFront.dbgUnwind = true;
      ir = CodeGenFront.genIR(e, ir, osrc);
  
      //System.out.println("* 6. backend codegen");
      //CodeGenBack.dbg = true;
      CodeGenBack.compile(ir);
      
      ir.accumulateGlobals();
    }
    TAC.dbgResolveRefs = true;

    //System.out.println("* link");
    //Linker.dbg = true;
    Executable exe = Linker.link(ir, ramOffs, constOffs, extDefs, true);
    
    //System.out.println(".. all ok, " + exe.machineCode.length + " bytes of code, pc start @ 0x" + Integer.toHexString(exe.getPCStart()));
    
    return exe;
  }
  
  public static void main(String[] args) {
    String src = 
        "module mandel;\n" +
        "println('*************');\n" + 
        "println('program start');\n" + 
        "println('crc32:' + crc.crc32(0, 'abcd', 4));\n" + 
        "mul = 1000;\n" +
        "step = 0.2;\n" +

        "func calcIterations(x, y) {\n" +
        "  zr = x; zi = y; i = 0;\n" +
        "  while (++i < mul) {\n" +
        "    zr2 = zr*zr; zi2 = zi*zi;\n" +
        "    if (zr2 + zi2 > 2*2) break;\n" + //return i;\n" +
        "    zrt = zr;\n" +
        "    zr = zr2 - zi2 + x;\n" +
        "    zi = 2*zrt*zi + y;\n" +
        "  }" +
        "  return i;\n" +
        "}" +
        
        "func fib(n) {\n" +
        "  if (n == 0) return 0;\n" +
        "  else if (n == 1) return 1;\n" + 
        "  else return fib(n - 1) + fib(n - 2);\n"  +
        "}\n" +

        "output = '';\n" + 
        "for (y in -1.0 # step # 1.0) {\n" +
        "  for (x in -1.6 # step/2 # 0.4) {\n" +
        "    iters = calcIterations(x,y);\n" +
        "    if (iters == mul) output = output + '0';\n" +
        "    else                output = output + '1';\n" +
        "  }\n" +
        "  output = output + '\n';\n" + 
        "}\n" +
        "println('cos(0.1):' + cos(0.1));\n" +
        "print('mandel\n' + output);\n" +
        "anon = {println('calling anon');return 'hello anon ';};\n" +
        "mojja = fib;\n" +
        "println('fibo='+mojja(12));\n" +
        "println(mandel.anon + ':' + anon());\n" +
        "println('sisterglobal:' + otherglobal);\n" +
        "println('friendglobal:' + walnut.otherglobal);\n" +
        "sisfun = sisterfunc();\n" +
        "println('sisterfunc return value:'  + sisfun);\n" + 
        "println('calling it:' + sisfun());\n" +
        "false = 0;\n" +
        "true = !false;\n" +
        "println('true: ' + true);\n" +
        "println('false:' + false);\n" +
        "if (true) walnut.friendfunc();\n" +
        "if (!true) halt();\n" +
        "list = [1,2,3,4];\n" +
        "println(list[3]);\n" +
        "println('walnut.lfunc[0]():' + walnut.lfunc[0]());\n" + 
        "println('walnut.lfunc[1]():' + walnut.lfunc[1]());\n" + 
        "println('walnut.lfunc[2]():' + walnut.lfunc[2]());\n" +
        "string = 'pelle';\n" +
        "println('pelle'[0]);\n" + 
        "println(string[1]);\n" + 
        "println(string[2]);\n" + 
        "println(string[3]);\n" + 
        "println(string[4]);\n" + 
        "println(string);\n" +
        "string += 'rockar';\n" +
        "println(string);\n" +
        "string -= 'rockar';\n" +
        "println(string);\n" +
        "arr[];\n" +
        "println(arr);\n" +
        "arr = arr + 'some end';\n" +
        "println(arr);\n" +
        "arr = 'begin' + arr;\n" +
        "println(arr);\n" +
        "arr += 'end';\n" +
        "println(arr);\n" +
        "arr++;\n" +
        "println(arr);\n" +
        "++arr;\n" +
        "println(arr);\n" +
        "println(len(arr));\n" +
        "arr[1] = 'middle';\n" +
        "arr[4] = arr[2];\n" +
        "arr[3] = 'pre' + str(arr[4]);\n" +
        "arr[2] = 'after middle';\n" +
        "println(arr);\n" +
        "for (i = 0; i < len(arr); i++) {\n" +
        "  println(i + ': ' + arr[i]);\n" +
        "  arr[i] = i;\n" +
        "}\n" +
        "for (i = 0; i < len(arr); i++) {\n" +
        "  println(i + ': ' + arr[i]);\n" +
        "}\n" +
        "word = 'Are we not drawn onward, we few, drawn onward to new era?';\n" +
        "rev = '';\n" +
        "for (i = len(word) - 1; i >= 0; i--) {\n" +
        "  rev += word[i];\n" +
        "}\n" +
        "println(word, ' |fornext| ', rev);\n" +
        "rev = '';\n" +
        "for (c in word) {\n" +
        "  rev = c + rev;\n" +
        "}\n" +
        "println(word + ' |foreach| ' + rev);\n" +
        "println(word + ' | range | ' + word[len(word)-1 # 0]);\n" +

        
        "ident = 1;\n" +
        "multi = 5;\n" +
        "matrix = [[ident,ident+multi,ident*multi],[2*(ident),fib(12),2*(ident*multi)],[3*ident,3*(ident+multi),3*(ident*multi)]];\n" +
        "println(matrix);\n" +
        "matrix[1][1] = 5000;\n" +
        "println(matrix);\n" +
        "func getarr(l) { l2 = l; l2[2] = 9; return l2; };\n" +
        "modarr = getarr([5,4,3,2,1]);\n" +
        "println(modarr);\n" +
        "iniarr = [5,4,3,2,1];\n" +
        "println(getarr(iniarr)[0]);\n" +
        "println(getarr(iniarr)[2]);\n" +
        "println(getarr(iniarr)[4]);\n" +
        "ix = 0;\n" +
        "println('iniarr[' + ix + ']:' + iniarr[ix]);\n" +
        "ix = 'a';\n" +
        "println('iniarr[' + ix + ']:' + iniarr[ix]);\n" +
        "map = ['call':{println('mapfunc called');}, 'data':123];\n" +
        "map.call();\n" +
        "println(walnut.ident);\n" +
        
        
        
        "walnut.ident = 5;\n" +
        
        
        
        "println('in mandel : walnut.ident:' + walnut.ident);\n" +
        "println('in mandel : walnut.otherfunc()');\n" +
        "walnut.otherfunc();\n" +
        "println('in mandel : walnut.walnutmap.call()');\n" +
        "walnut.walnutmap.call();\n" +
        "println('in mandel : walnut.walnutmap[\"call\"]()');\n" +
        "walnut.walnutmap['call']();\n" +
        "println('in mandel : walnut.walnutmap.call = mandel.fib');\n" +
        "walnut.walnutmap.call = fib;\n" +
        "println('in mandel : walnut.walnutmap.call(12)');\n" +
        "println(walnut.walnutmap.call(12));\n" +
        "func add(x, y) { println('add' + x + '+' + y); return x+y; }\n" +
        "walnut.fmap.call();\n" +
        "walnut.fmap.call = mandel.add;\n" +
        "println(walnut.fmap.call(2,3));\n" +
        "walnut.fmap.sub.call();\n" +

        "walnut.fmap.sub.call = mandel.add;\n" +
        "println(walnut.fmap.sub.call(4,5));\n" +
        
        "map.call = fib;\n" +
        "println(map.call(12));\n" +
        "map.call = walnut.fmap.sub.call;\n" +
        "println(map.call(6,7));\n" +
        "println(-270);\n" +
        "println(('abcdef'[0#3])[2#0]);\n" +
        "string = 'peter';\n" +
        "string[2] = nil;\n" +
        "println(string);\n" +
        "farg = { \n" +
        "  println('argc   :' + $argc);\n" +
        "  println('argv   :' + str($argv));\n" +
        "  println('argv[0]:' + $argv[0]);\n" +
        "  println('argv[1]:' + $argv[1]);\n" +
        "  println('argv[2]:' + str($argv[2]));\n" +
        "  for (a in $argv) { \n" +
        "    println('argv :' + str(a));\n" +
        "  }\n" +
        "  for (a in $argv)\n" +
        "    println('argv :' + str(a));\n" +
        "  println('arg0   :' + $0);\n" +
        "  println('arg1   :' + $1);\n" +
        "  println('arg2   :' + str($2));\n" +
        "  println('arg3   :' + $3);\n" +
        "};\n" +
        "farg('mo','bo', ['ko','ko2']);\n" +
        "farg();\n" +
        "a = ['init':{ for(x in 0#10) {println($0+x);} }, 'exe':{println('hello world');}, 'nbr':123];  a.init(1);\n" +
        "func rec(o) {\n" +
        "  if (len(o) == 0) {\n" +
        "    println(o);\n" +
        "  } else {" +
        "    for (x in o) rec(x);\n" +
        "  }\n" +
        "}\n" +
        "majs = [0,1,2,[3,4,[5,6],7],8];\n" +
        "rec(majs);\n" +
        "a = true;\n" +
        "if (a) println('a true'); else for (x in 0#10) println('a false');\n" +
        "a = 1>2;\n" +
        "println('1>2=' + a);\n" +
        "a = 1<2;\n" +
        "println('1<2=' + a);\n" +
        "arrb = [0,1,2,3,4,5,6,7];\n"+
        "arr = arrb[{if ($0 > 4) return $0; else return nil;}];\n"+
        "println(arr);\n" +
        "arr = arrb[{return $0*2;}];\n"+
        "println(arr);\n" +
        "arrb[[1,2,3]] = 4;\n"+
        "println(arrb);\n" +
        "arrb = arrb[{if ($0 != 4) return $0; else return nil;}];\n"+
        "println(arrb);\n" +
        "__BKPT;\n" +
        ""
        ;
    
    // DONE:   ambiguity: modulename / variablename when x.key() for x - always prefer variable name in frontend
    // DONE:   add 'return' to functions and anon if not explicitly set by programmer
    // DONE:   add number of call arguments to stack frame, ie push pc+fp and also argc
    // DONE:   in frontend, reverse argument list in order to be able to handle varargs. Fix backend for this also
    // DONE:   use of variables before declaration within scope should not work, confusing 
    // DONE:   ranges 
    // DONE:   partialarr = oldarr[[3,4,5]], partialarr = oldarr[3#5]
    // DONE:   a=[]; for(i in 0#10) {a['x'+i]='y'+i;} // inverts key/val
    // DONE:   if (a) println('a true'); else println('a false');
    // DONE:   { globalscope = 1; { localscope = 2; anon = { return localscope * 4; }; println(anon()); } }
    // DONE:   a = 1>2;
    // DONE:   arr = arrb[{if ($0 > 4) return $0; else return nil;}]; // removes all elements below 4
    // DONE:   arr = arrb[{return $0*2;}]; // multiplies all elements by 2
    // DONE:   arr[[1,2,3]] = 4
    // DONE:   map = map[{if $0 ...etc}]
    // DONE:  "r = ['a':1,'b':2,'c':3];\n" +
    //        "r.b = r;\n" +
    //        "println(r.b.b['b'].c);\n" +
    // FIXME:  goto
    // FIXME:  handle 'global' keyword
    // FIXME:  on $0..999, in StructAnalyser, replace these by internal variables so we do not need to check range all the time


    String siblingsrc = 
        "module mandel;"+
        "otherglobal = 'variable sister';\n" + 
        "func sisterfunc() {" +
        "  return {println('sisterfunc anon func called');return 'it worked';};\n" +
        "}" +
        "func argfunc(a,b,c) {" +
        "  println('1st:' + a);\n" +
        "  println('2nd:' + b);\n" +
        "  println('3rd:' + c);\n" +
        "}";
    String othersrc = 
         "module walnut;\n" +
         "otherglobal = 'variable friend';\n" + 
         "l = [1,2,3];\n" +
         "l2 = [1, ['a'], 'b', ['c', ['dekla']]];\n" +
         "l3 = [];\n" +
         "l4[];\n" + 
         "func otherfunc() {println('walnut.otherfunc called');}\n" +
         "lfunc = [ \n"+
         "          {return 760;}, {return 401;}, {return 293;}\n"+
         "        ];\n"+
         "walnutmap = ['call':{println('walnut mapfunc called');}, 'data':123];\n" +
         "walnutmap.call();\n" +
         "val=lfunc[0]();" +
         "println('val='+val);\n" + 
         "val=lfunc[1]();" +
         "println('val='+val);\n" + 
         "val=lfunc[2]();" +
         "println('val='+val);\n" + 
         "println('l[0]=1?' + l[0]);\n" +
         "println(l2);\n" +
         "println('l2[3][1][0][3]=' + l2[3][1][0][3] + '(' + char(l2[3][1][0][3]) + ')');\n" +
         "println('l:' + str(l));\n" +
         "println('l2:' + str(l2));\n" +
         "println('l3:' + str(l3));\n" +
         "println('l4:' + str(l4));\n" +
         "println('lfunc:' + str(lfunc));\n" +
         "ident = 4;\n" +
         "fmap = ['call' : {println('invoked walnut.fmap.call');}, 'sub':['call' : {println('invoked walnut.fmap.sub.call');}]];\n" +
         "func friendfunc() {" +
         "  println('friendfunc called ' + mandel.anon);\n" +
         "}";

    String crcsrc = "module crc;\n" +
  "crc32_tab = [\n" +
  "0x00000000, 0x77073096, 0xee0e612c, 0x990951ba, 0x076dc419, 0x706af48f,\n" +
  "0xe963a535, 0x9e6495a3, 0x0edb8832, 0x79dcb8a4, 0xe0d5e91e, 0x97d2d988,\n" +
  "0x09b64c2b, 0x7eb17cbd, 0xe7b82d07, 0x90bf1d91, 0x1db71064, 0x6ab020f2,\n" +
  "0xf3b97148, 0x84be41de, 0x1adad47d, 0x6ddde4eb, 0xf4d4b551, 0x83d385c7,\n" +
  "0x136c9856, 0x646ba8c0, 0xfd62f97a, 0x8a65c9ec, 0x14015c4f, 0x63066cd9,\n" +
  "0xfa0f3d63, 0x8d080df5, 0x3b6e20c8, 0x4c69105e, 0xd56041e4, 0xa2677172,\n" +
  "0x3c03e4d1, 0x4b04d447, 0xd20d85fd, 0xa50ab56b, 0x35b5a8fa, 0x42b2986c,\n" +
  "0xdbbbc9d6, 0xacbcf940, 0x32d86ce3, 0x45df5c75, 0xdcd60dcf, 0xabd13d59,\n" +
  "0x26d930ac, 0x51de003a, 0xc8d75180, 0xbfd06116, 0x21b4f4b5, 0x56b3c423,\n" +
  "0xcfba9599, 0xb8bda50f, 0x2802b89e, 0x5f058808, 0xc60cd9b2, 0xb10be924,\n" +
  "0x2f6f7c87, 0x58684c11, 0xc1611dab, 0xb6662d3d, 0x76dc4190, 0x01db7106,\n" +
  "0x98d220bc, 0xefd5102a, 0x71b18589, 0x06b6b51f, 0x9fbfe4a5, 0xe8b8d433,\n" +
  "0x7807c9a2, 0x0f00f934, 0x9609a88e, 0xe10e9818, 0x7f6a0dbb, 0x086d3d2d,\n" +
  "0x91646c97, 0xe6635c01, 0x6b6b51f4, 0x1c6c6162, 0x856530d8, 0xf262004e,\n" +
  "0x6c0695ed, 0x1b01a57b, 0x8208f4c1, 0xf50fc457, 0x65b0d9c6, 0x12b7e950,\n" +
  "0x8bbeb8ea, 0xfcb9887c, 0x62dd1ddf, 0x15da2d49, 0x8cd37cf3, 0xfbd44c65,\n" +
  "0x4db26158, 0x3ab551ce, 0xa3bc0074, 0xd4bb30e2, 0x4adfa541, 0x3dd895d7,\n" +
  "0xa4d1c46d, 0xd3d6f4fb, 0x4369e96a, 0x346ed9fc, 0xad678846, 0xda60b8d0,\n" +
  "0x44042d73, 0x33031de5, 0xaa0a4c5f, 0xdd0d7cc9, 0x5005713c, 0x270241aa,\n" +
  "0xbe0b1010, 0xc90c2086, 0x5768b525, 0x206f85b3, 0xb966d409, 0xce61e49f,\n" +
  "0x5edef90e, 0x29d9c998, 0xb0d09822, 0xc7d7a8b4, 0x59b33d17, 0x2eb40d81,\n" +
  "0xb7bd5c3b, 0xc0ba6cad, 0xedb88320, 0x9abfb3b6, 0x03b6e20c, 0x74b1d29a,\n" +
  "0xead54739, 0x9dd277af, 0x04db2615, 0x73dc1683, 0xe3630b12, 0x94643b84,\n" +
  "0x0d6d6a3e, 0x7a6a5aa8, 0xe40ecf0b, 0x9309ff9d, 0x0a00ae27, 0x7d079eb1,\n" +
  "0xf00f9344, 0x8708a3d2, 0x1e01f268, 0x6906c2fe, 0xf762575d, 0x806567cb,\n" +
  "0x196c3671, 0x6e6b06e7, 0xfed41b76, 0x89d32be0, 0x10da7a5a, 0x67dd4acc,\n" +
  "0xf9b9df6f, 0x8ebeeff9, 0x17b7be43, 0x60b08ed5, 0xd6d6a3e8, 0xa1d1937e,\n" +
  "0x38d8c2c4, 0x4fdff252, 0xd1bb67f1, 0xa6bc5767, 0x3fb506dd, 0x48b2364b,\n" +
  "0xd80d2bda, 0xaf0a1b4c, 0x36034af6, 0x41047a60, 0xdf60efc3, 0xa867df55,\n" +
  "0x316e8eef, 0x4669be79, 0xcb61b38c, 0xbc66831a, 0x256fd2a0, 0x5268e236,\n" +
  "0xcc0c7795, 0xbb0b4703, 0x220216b9, 0x5505262f, 0xc5ba3bbe, 0xb2bd0b28,\n" +
  "0x2bb45a92, 0x5cb36a04, 0xc2d7ffa7, 0xb5d0cf31, 0x2cd99e8b, 0x5bdeae1d,\n" +
  "0x9b64c2b0, 0xec63f226, 0x756aa39c, 0x026d930a, 0x9c0906a9, 0xeb0e363f,\n" +
  "0x72076785, 0x05005713, 0x95bf4a82, 0xe2b87a14, 0x7bb12bae, 0x0cb61b38,\n" +
  "0x92d28e9b, 0xe5d5be0d, 0x7cdcefb7, 0x0bdbdf21, 0x86d3d2d4, 0xf1d4e242,\n" +
  "0x68ddb3f8, 0x1fda836e, 0x81be16cd, 0xf6b9265b, 0x6fb077e1, 0x18b74777,\n" +
  "0x88085ae6, 0xff0f6a70, 0x66063bca, 0x11010b5c, 0x8f659eff, 0xf862ae69,\n" +
  "0x616bffd3, 0x166ccf45, 0xa00ae278, 0xd70dd2ee, 0x4e048354, 0x3903b3c2,\n" +
  "0xa7672661, 0xd06016f7, 0x4969474d, 0x3e6e77db, 0xaed16a4a, 0xd9d65adc,\n" +
  "0x40df0b66, 0x37d83bf0, 0xa9bcae53, 0xdebb9ec5, 0x47b2cf7f, 0x30b5ffe9,\n" +
  "0xbdbdf21c, 0xcabac28a, 0x53b39330, 0x24b4a3a6, 0xbad03605, 0xcdd70693,\n" +
  "0x54de5729, 0x23d967bf, 0xb3667a2e, 0xc4614ab8, 0x5d681b02, 0x2a6f2b94,\n" +
  "0xb40bbe37, 0xc30c8ea1, 0x5a05df1b, 0x2d02ef8d" +
  "];\n" +
  "\n" +
  "func crc32(crc, buf, size) { \n" +
  "  i = 0;\n" +
  "  crc = crc ^ ~0;\n" +
  "  while (size--) {\n" +
  "   crc = crc32_tab[(crc ^ int(buf[i++])) & 0xff] ^ (crc >> 8);\n"+
  "  }\n" +
  " return crc ^ ~0;\n" +
  "}\n";


//    src = 
//        "arr = [1,2,3,4,5];\n"+
//        "for (i in -3#8) {\n" +
//        " if (i in arr) println('found', i);\n"+
//        " else          println('did not find',i);\n"+
//        "}\n"+
//        "if ('korv' in 'bajskorv') println('found korv');\n"+
//        "if ('dore' in 'bajskorv') println('found dore');\n"+
//        "";
//   othersrc = siblingsrc = crcsrc = ""; 
   
    Map<String, ExtCall> extDefs = new HashMap<String, ExtCall>();
    Processor.addCommonExtdefs(extDefs);
    extDefs.put("cos", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        return new Processor.M((float)Math.cos(args[0].f));
      }
    });
    
    Executable e = null;
    try {
      e = Compiler.compile(extDefs, 0x0000, 0x4000, crcsrc, othersrc, siblingsrc, src);
    } catch (CompilerError ce) {
      String s = Compiler.getSource();
      int strstart = Math.min(s.length(), Math.max(0, ce.getStringStart()));
      int strend = Math.min(s.length(), Math.max(0, ce.getStringEnd()));
      if (strstart > 0) {
        int ps = Math.min(s.length(), Math.max(0, strstart - 50));
        int pe = Math.max(0, Math.min(s.length(), strend + 50));
        System.out.println(ce.getMessage());
        System.out.println("... " + s.substring(ps, strstart) + 
            " -->" + s.substring(strstart, strend) + "<-- " +
            s.substring(strend, pe) + " ...");
      }
      throw ce;
    }
    Processor p = new Processor(0x10000, e);
    //Processor.dbgRun = true;
    //Processor.dbgMem = true;
    int i = 0;
    try {
      for (; i < 10000000*2; i++) {
        p.step();
      }
      throw new ProcessorError("processor hanged");
    } catch (ProcessorFinishedError pfe) {
      System.out.println("processor end, retval " + pfe.getRet());
    }
    catch (ProcessorError pe) {
      System.out.println("**********************************************");
      System.out.println(String.format("Exception at pc 0x%06x", p.getPC()));
      System.out.println(p.getProcInfo());
      System.out.println(pe.getMessage());
      System.out.println("**********************************************");
      System.out.println("DISASM");
      Assembler.disasm(System.out, "   ", p.getExecutable().getMachineCode(), p.getPC(), 8);
      System.out.println("STACK");
      p.printStack(System.out, "   ", 16);
      pe.printStackTrace(System.err);
    }
    System.out.println(i + " instructions executed");
  }

  public static String getSource() {
    return src;
  }
  
  Map<String, ExtCall> extDefs;
  Linker linker;
  public Compiler(Map<String, ExtCall> extDefs, int ramOffs, int constOffs) {
    this.extDefs = extDefs;
    linker = new Linker(ramOffs, constOffs);
  }
  
  IntermediateRepresentation ir = null;
  public Executable compileIncrementally(String src) {
    return compileIncrementally(new SourceString("<string>", src));
  }
  public Executable compileIncrementally(Source src) {
    Executable exe = null;
    try {
      ASTNodeBlok e = AST.buildTree(src.getSource());
      ASTOptimiser.optimise(e);
      Grammar.check(e);
      StructAnalysis.analyse(e, ir);
      ir = CodeGenFront.genIR(e, ir, src);
      CodeGenBack.compile(ir);
      ir.accumulateGlobals();
      exe = linker.link(ir, extDefs, true);
    } finally {
      if (ir != null) ir.clearModules();
    }
    return exe;
  }
}
