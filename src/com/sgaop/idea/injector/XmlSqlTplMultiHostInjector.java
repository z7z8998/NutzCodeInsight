package com.sgaop.idea.injector;

import com.intellij.lang.Language;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.xml.XmlTag;
import com.sgaop.util.DomUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * @author 黄川 huchuc@vip.qq.com
 * @date: 2019/4/1
 */
public class XmlSqlTplMultiHostInjector implements MultiHostInjector {

    static final Language SQL_LANGUAGE = Language.findLanguageByID("SQL");
    static final String SQL_TAG = "sql";
//    static final String EXP_TAG = "exp";

    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement psiElement) {
        if (DomUtil.isNutzSqlFile(psiElement.getContainingFile())) {
            if (psiElement instanceof XmlTag) {
                XmlTag tag = (XmlTag) psiElement;
                if (SQL_TAG.equals(tag.getName())) {
                    registrarInjecting(SQL_LANGUAGE, registrar, DomUtil.findXmlTexts(psiElement.getChildren()), null, null);
                }
//                else if (EXP_TAG.equals(tag.getName())) {
//                    registrarInjecting(XMLLanguage.INSTANCE, registrar, DomUtil.findXmlTexts(psiElement.getChildren()), "<!--", "-->");
//                }
            }
        }
    }

    private void registrarInjecting(Language language, MultiHostRegistrar registrar, List<PsiElement> els, String prefix, String suffix) {
        if (els.size() > 0) {
            registrar.startInjecting(language);
            els.forEach(el -> registrar.addPlace(prefix, suffix, (PsiLanguageInjectionHost) el, ElementManipulators.getValueTextRange(el)));
            registrar.doneInjecting();
        }
    }


    @NotNull
    @Override
    public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
        return Collections.singletonList(XmlTag.class);
    }
}
