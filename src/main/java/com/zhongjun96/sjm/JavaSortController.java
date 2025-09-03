package com.zhongjun96.sjm;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/java")
public class JavaSortController {

    @PostMapping("/sort")
    public String sortJava(@RequestBody Map<String, String> request) {
        String aCode = request.get("aCode");
        String bCode = request.get("bCode");

        CompilationUnit cuA = StaticJavaParser.parse(aCode);
        CompilationUnit cuB = StaticJavaParser.parse(bCode);
        LexicalPreservingPrinter.setup(cuB);

        ClassOrInterfaceDeclaration classA = cuA.findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() -> new IllegalArgumentException("A 中未找到类"));
        ClassOrInterfaceDeclaration classB = cuB.findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() -> new IllegalArgumentException("B 中未找到类"));

        // 1. 获取 A 的方法顺序（用签名区分重载）
        List<String> methodOrder = classA.getMethods().stream()
                .map(m -> m.getSignature().asString())
                .collect(Collectors.toList());

        // 2. 建立 B 的方法映射（签名 -> 方法节点列表，支持重载）
        Map<String, List<MethodDeclaration>> methodMap = new LinkedHashMap<>();
        for (MethodDeclaration m : classB.getMethods()) {
            methodMap.computeIfAbsent(m.getSignature().asString(), k -> new ArrayList<>()).add(m);
        }

        // 3. 按 A 的顺序提取 B 中的方法（直接使用原始节点，避免丢失缩进/注释）
        List<MethodDeclaration> sortedMethods = new ArrayList<>();
        for (String sig : methodOrder) {
            List<MethodDeclaration> list = methodMap.remove(sig);
            if (list != null) {
                sortedMethods.addAll(list);
            }
        }

        // 4. 把 B 中 A 没有的（额外方法/重载）追加到最后
        for (List<MethodDeclaration> list : methodMap.values()) {
            sortedMethods.addAll(list);
        }

        // 5. 重新构建 members：保留非方法成员不动，把方法替换成新的顺序
        NodeList<BodyDeclaration<?>> newMembers = new NodeList<>();
        for (BodyDeclaration<?> member : classB.getMembers()) {
            if (!(member instanceof MethodDeclaration)) {
                newMembers.add(member); // 非方法成员原样保留
            }
        }
        newMembers.addAll(sortedMethods);

        classB.setMembers(newMembers);

        // 6. 返回带原始缩进/注释的源码
        return LexicalPreservingPrinter.print(cuB);
    }

}
