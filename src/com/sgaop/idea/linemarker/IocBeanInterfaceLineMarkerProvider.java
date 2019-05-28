package com.sgaop.idea.linemarker;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.module.impl.scopes.ModuleWithDependenciesScope;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.java.stubs.index.JavaAnnotationIndex;
import com.intellij.psi.impl.java.stubs.index.JavaSuperClassNameOccurenceIndex;
import com.intellij.psi.impl.source.PsiClassImpl;
import com.intellij.psi.impl.source.PsiTypeElementImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTypesUtil;
import com.sgaop.idea.linemarker.navigation.IocBeanInterfaceNavigationHandler;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * @author huchuc@vip.qq.com
 * @date: 2019/5/23
 */
public class IocBeanInterfaceLineMarkerProvider extends LineMarkerProviderDescriptor {

    static final String INJECT_QUALI_FIED_NAME = "org.nutz.ioc.loader.annotation.Inject";
    static final String IOCBEAN_QUALI_FIED_NAME = "org.nutz.ioc.loader.annotation.IocBean";
    static final int SENSITIVE_MAX = 3;
    static int sensitive = 0;
    static HashMap<String, List<PsiElement>> methodIocBeans = new HashMap<>();
    Icon icon = AllIcons.Javaee.InterceptorClass;

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement psiElement) {
        try {
            PsiField psiFiled = this.getPsiFiled(psiElement);
            PsiAnnotation psiAnnotation = this.getPsiAnnotation(psiFiled);
            GlobalSearchScope searchScope = psiElement.getResolveScope();
            if (psiFiled != null && psiAnnotation != null && searchScope instanceof ModuleWithDependenciesScope) {
                GlobalSearchScope moduleScope = ((ModuleWithDependenciesScope) searchScope).getModule().getModuleScope();
                PsiTypeElementImpl psiTypeElement = this.getPsiTypeElement(psiAnnotation);
                PsiClass psiClass = PsiTypesUtil.getPsiClass(psiTypeElement.getType());
                String name = psiClass.getName();
                List<PsiElement> list = this.getImplListElements(name, psiClass.getQualifiedName(), psiElement, moduleScope);
                if (list.size() > 0) {
                    return new LineMarkerInfo<>(psiTypeElement, psiTypeElement.getTextRange(), icon,
                            new FunctionTooltip(MessageFormat.format("快速跳转至 {0} 的 @IocBean 实现类", name)),
                            new IocBeanInterfaceNavigationHandler(name, list),
                            GutterIconRenderer.Alignment.LEFT);
                }
            }
        } catch (Exception e) {
            //忽略异常
        }
        return null;
    }


    private PsiField getPsiFiled(@NotNull PsiElement psiElement) {
        if (psiElement instanceof PsiIdentifier) {
            if (psiElement.getParent() instanceof PsiJavaCodeReferenceElement) {
                if (psiElement.getParent().getParent() instanceof PsiTypeElement) {
                    if (psiElement.getParent().getParent().getParent() instanceof PsiField) {
                        return (PsiField) psiElement.getParent().getParent().getParent();
                    }
                }
            }
        }
        return null;
    }

    private PsiAnnotation getPsiAnnotation(PsiField psiField) {
        if (psiField != null) {
            PsiAnnotation psiAnnotation = psiField.getAnnotation(INJECT_QUALI_FIED_NAME);
            if (psiAnnotation != null) {
                return psiAnnotation;
            }
        }
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result) {
        if (elements.size() > 0) {
            try {
                PsiElement psiElement = elements.get(0).getContext();
                GlobalSearchScope moduleScope = ((ModuleWithDependenciesScope) psiElement.getResolveScope()).getModule().getModuleScope();
                this.init(psiElement, moduleScope);
            } catch (Exception e) {
            }
        }
    }

    private void init(@NotNull PsiElement psiElement, GlobalSearchScope moduleScope) {
        if (sensitive == 0) {
            sensitive++;
            methodIocBeans = this.getMethodIocBeans(psiElement.getProject(), moduleScope);
        } else {
            sensitive++;
            if (sensitive >= SENSITIVE_MAX) {
                sensitive = 0;
            }
        }
    }

    /**
     * 取得实现类
     *
     * @param psiElement
     * @return
     */
    private List<PsiElement> getImplListElements(String name, String qualifiedName, PsiElement psiElement, GlobalSearchScope moduleScope) {
        Project project = psiElement.getProject();
        Collection<PsiReferenceList> psiReferenceListCollection = JavaSuperClassNameOccurenceIndex.getInstance().get(name, project, moduleScope);
        List<PsiElement> elements = new ArrayList<>();
        for (PsiReferenceList psiReferenceList : psiReferenceListCollection) {
            PsiClassImpl psiClassImpl = (PsiClassImpl) psiReferenceList.getContext();
            if (psiClassImpl.getAnnotation(IOCBEAN_QUALI_FIED_NAME) != null) {
                elements.add(psiClassImpl);
            }
        }
        if (methodIocBeans.containsKey(qualifiedName)) {
            elements.addAll(methodIocBeans.get(qualifiedName));
        }
        return elements;
    }

    private HashMap<String, List<PsiElement>> getMethodIocBeans(Project project, GlobalSearchScope moduleScope) {
        HashMap<String, List<PsiElement>> elementHashMap = new HashMap<>(1);
        Collection<PsiAnnotation> psiAnnotationCollection = JavaAnnotationIndex.getInstance().get("IocBean", project, moduleScope);
        for (PsiAnnotation psiAnnotation : psiAnnotationCollection) {
            if (psiAnnotation.getParent() instanceof PsiModifierList) {
                if (psiAnnotation.getParent().getParent() instanceof PsiMethod) {
                    PsiMethod method = (PsiMethod) psiAnnotation.getParent().getParent();
                    String returnQualifiedName = method.getReturnType().getCanonicalText();
                    if (elementHashMap.containsKey(returnQualifiedName)) {
                        elementHashMap.get(returnQualifiedName).add(method);
                    } else {
                        List<PsiElement> arr = new ArrayList<>();
                        arr.add(method);
                        elementHashMap.put(returnQualifiedName, arr);
                    }
                }
            }
        }
        return elementHashMap;
    }

    private PsiTypeElementImpl getPsiTypeElement(PsiElement psiElement) {
        PsiElement[] psiElements = psiElement.getParent().getParent().getChildren();
        for (PsiElement element : psiElements) {
            if (element instanceof PsiTypeElement) {
                return (PsiTypeElementImpl) element;
            }
        }
        return null;
    }


    @Nls(capitalization = Nls.Capitalization.Sentence)
    @Nullable("null means disabled")
    @Override
    public String getName() {
        return "navigate to Implementation class";
    }
}