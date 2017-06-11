package com.github.ustc_zzzz.authlibloginhelper.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.*;

/**
 * @author ustc_zzzz
 */
public class AuthlibLoginHelperTransformer implements IClassTransformer
{
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass)
    {
        if (transformedName.startsWith("net.minecraft.client.multiplayer.GuiConnecting"))
        {
            return transformGuiConnecting(basicClass);
        }
        return basicClass;
    }

    private byte[] transformGuiConnecting(byte[] basicClass)
    {
        ClassReader classReader = new ClassReader(basicClass);
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
        classReader.accept(new GuiConnectingVisitor(classWriter), ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
    }

    private static class GuiConnectingVisitor extends ClassVisitor
    {
        private GuiConnectingVisitor(ClassVisitor cv)
        {
            super(Opcodes.ASM5, cv);
        }

        @Override
        public MethodVisitor visitMethod(final int a, final String n, final String d, final String s, final String[] e)
        {
            MethodVisitor parent = super.visitMethod(a, n, d, s, e);
            return new MethodVisitor(Opcodes.ASM5, parent)
            {
                private Logger logger = LogManager.getLogger("authlibloginhelper");
                private String className = Type.getInternalName(AuthlibLoginHelperHooks.class);

                @Override
                public void visitCode()
                {
                    if ("connect".equals(n) || "func_146367_a".equals(n))
                    {
                        this.logger.info("AuthlibLoginHelper: Injecting codes into GuiConnecting (1/2) ...");
                        super.visitVarInsn(Opcodes.ALOAD, 1);
                        super.visitVarInsn(Opcodes.ILOAD, 2);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, this.className, "connectCallback", d, false);
                    }
                }

                @Override
                public void visitTypeInsn(int opcode, String type)
                {
                    if (Opcodes.NEW == opcode && "net/minecraft/client/network/NetHandlerLoginClient".equals(type))
                    {
                        this.logger.info("AuthlibLoginHelper: Injecting codes into GuiConnecting (2/2) ...");
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, this.className, "loginCallback", "()V", false);
                    }
                    super.visitTypeInsn(opcode, type);
                }
            };
        }
    }
}
