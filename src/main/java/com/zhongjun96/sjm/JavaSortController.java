package com.zhongjun96.sjm;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/java")
public class JavaSortController {

    @PostMapping("/sort")
    public String sortJava(@RequestBody Map<String, String> request) {
        String aCode = request.get("aCode");  // A 文件源码
        String bCode = request.get("bCode");  // B 文件源码

        CompilationUnit cuA = StaticJavaParser.parse(aCode);
        CompilationUnit cuB = StaticJavaParser.parse(bCode);

        // 保留原始格式
        LexicalPreservingPrinter.setup(cuB);

        // 假设每个文件只有一个顶层类
        ClassOrInterfaceDeclaration classA = cuA.findFirst(ClassOrInterfaceDeclaration.class).get();
        ClassOrInterfaceDeclaration classB = cuB.findFirst(ClassOrInterfaceDeclaration.class).get();

        // A 的方法顺序
        List<String> methodOrder = classA.getMethods().stream()
                .map(MethodDeclaration::getNameAsString)
                .collect(Collectors.toList());

        // B 的方法映射
        Map<String, MethodDeclaration> methodMap = new HashMap<>();
        for (MethodDeclaration m : classB.getMethods()) {
            methodMap.put(m.getNameAsString(), m);
        }

        // 按顺序重排
        List<MethodDeclaration> sortedMethods = new ArrayList<>();
        for (String name : methodOrder) {
            if (methodMap.containsKey(name)) {
                sortedMethods.add(methodMap.get(name));
            }
        }

        // B 中 A 没有的方法追加到最后
        for (MethodDeclaration m : classB.getMethods()) {
            if (!methodOrder.contains(m.getNameAsString())) {
                sortedMethods.add(m);
            }
        }

        // 清空并按顺序添加
        classB.getMembers().removeIf(member -> member instanceof MethodDeclaration);
        for (MethodDeclaration m : sortedMethods) {
            classB.addMember(m);
        }

        // 返回结果
        return LexicalPreservingPrinter.print(cuB);
    }
}
