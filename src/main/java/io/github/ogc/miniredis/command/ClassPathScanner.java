package io.github.ogc.miniredis.command;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 类路径扫描器 -- 启动时遍历 classpath,找出所有贴了 {@link CommandName} 的 {@link Command} 类,
 * 反射实例化,按注解里的名字建表返回。
 *
 * <p>难点:JVM 启动后没有"项目里有哪些类"的清单,类是按需加载的。
 * 扫描器要自己找 -- 通过 {@link ClassLoader#getResources} 拿到包对应的物理位置,
 * 再按 URL 协议分支处理:
 * <ul>
 *   <li>{@code file://} -- IDE 跑 / target/classes,目录形态,用 {@link File} 递归遍历</li>
 *   <li>{@code jar:file://} -- 打 fat jar 跑,压缩包形态,用 {@link JarFile} 遍历 entries</li>
 * </ul>
 *
 * <p>跳过规则:接口、抽象类(含注解本身)、没有 {@code @CommandName} 注解的类一律不注册。
 */
@Slf4j
public class ClassPathScanner {

    /**
     * 扫描指定包下所有带 {@link CommandName} 的 Command 实现类。
     *
     * @param basePackage 包名,如 "io.github.ogc.miniredis.command"
     * @return 命令名(大写) -> Command 实例
     */
    public static Map<String, Command> scan(String basePackage) {
        Map<String, Command> result = new HashMap<>();
        String path = basePackage.replace('.', '/');

        try {
            Enumeration<URL> resources = Thread.currentThread()
                    .getContextClassLoader()
                    .getResources(path);

            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                List<String> classNames = collectClassNames(url, path);
                for (String className : classNames) {
                    registerIfCommand(className, result);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("class path scan failed for " + basePackage, e);
        }

        log.info("scanned {} commands from package {}", result.size(), basePackage);
        return result;
    }

    /**
     * 按 URL 协议收集类全限定名。
     * file 协议 -> 遍历目录;jar 协议 -> 遍历 jar 条目。
     */
    private static List<String> collectClassNames(URL url, String packagePath) throws IOException {
        List<String> names = new ArrayList<>();

        if (url.getProtocol().equals("file")) {
            File dir = new File(url.getFile());
            if (dir.exists() && dir.isDirectory()) {
                scanDirectory(dir, packagePath, names);
            }
        } else if (url.getProtocol().equals("jar")) {
            scanJar(url, packagePath, names);
        }
        // 其他协议(vsf / jrt 等)暂不支持,学习项目用不到

        return names;
    }

    /**
     * 递归扫描目录,把每个 .class 文件转成类全限定名。
     * 例:.../command/GetCommand.class -> io.github.ogc.miniredis.command.GetCommand
     */
    private static void scanDirectory(File dir, String packagePath, List<String> out) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packagePath + "/" + file.getName(), out);
            } else if (file.getName().endsWith(".class")) {
                String simpleName = file.getName().substring(0, file.getName().length() - ".class".length());
                out.add(packagePath.replace('/', '.') + "." + simpleName);
            }
        }
    }

    /**
     * 扫描 jar 包内指定路径下的 .class 条目。
     */
    private static void scanJar(URL url, String packagePath, List<String> out) throws IOException {
        // jar:file:/path/to/x.jar!/io/github/... -> 提取 jar 文件路径
        String jarPath = url.getFile().split("!")[0];
        jarPath = jarPath.replace("file:", "");

        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith(packagePath + "/") && name.endsWith(".class")) {
                    String className = name.substring(0, name.length() - ".class".length()).replace('/', '.');
                    out.add(className);
                }
            }
        }
    }

    /**
     * 加载类,检查注解,符合条件就实例化注册。
     * 用 {@code Class.forName(name, false, loader)} 不触发类初始化,
     * 避免 CommandDispatcher 自身初始化时递归(它构造时正在 scan)。
     */
    private static void registerIfCommand(String className, Map<String, Command> out) {
        try {
            Class<?> clazz = Class.forName(className, false, Thread.currentThread().getContextClassLoader());

            // 跳过接口、抽象类(注解本身也是接口,会一并跳过)
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                return;
            }

            CommandName annotation = clazz.getAnnotation(CommandName.class);
            if (annotation == null) {
                return; // 没贴注解,不是命令
            }

            if (!Command.class.isAssignableFrom(clazz)) {
                log.warn("{} has @CommandName but doesn't implement Command, skipped", className);
                return;
            }

            // 反射实例化(要求无参构造器),此时会触发类初始化(加载 static 常量如 PONG)
            Command instance = (Command) clazz.getDeclaredConstructor().newInstance();
            out.put(annotation.value().toUpperCase(), instance);
            log.debug("registered command: {} -> {}", annotation.value(), className);
        } catch (ClassNotFoundException e) {
            log.warn("class not found during scan: {}", className, e);
        } catch (NoSuchMethodException e) {
            log.warn("{} has @CommandName but no no-arg constructor, skipped", className);
        } catch (Exception e) {
            log.warn("failed to register command: {}", className, e);
        }
    }
}
