package name.yalsooni.genius.runner.delegate.helper;

import name.yalsooni.boothelper.classloader.ClassList;
import name.yalsooni.boothelper.classloader.util.ClassListUtil;
import name.yalsooni.boothelper.util.Log;
import name.yalsooni.boothelper.util.file.ExtFileSearch;
import name.yalsooni.genius.runner.definition.ErrCode;
import name.yalsooni.genius.runner.definition.annotation.Delegate;
import name.yalsooni.genius.runner.definition.annotation.Entry;
import name.yalsooni.genius.runner.definition.property.GeniusProperties;
import name.yalsooni.genius.runner.delegate.vo.DelegateDTO;
import name.yalsooni.genius.runner.delegate.vo.EntryDTO;
import name.yalsooni.genius.runner.repository.DelegateList;
import name.yalsooni.genius.runner.repository.GeniusClassLoader;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * 지정된 디렉토리의 JAR파일의 클래스를 검색하여 Delegate 추출한다.
 * Created by yoon-iljoong on 2016. 10. 31..
 */
public class GeniusDelegateEjector {

    private ExtFileSearch fileSearch = new ExtFileSearch(".jar");

    /**
     * 딜리게이트 어노테이션 추출
     * @param gProperties 지니어스 프로퍼티
     * @return DelegateList
     */
    public DelegateList eject(GeniusProperties gProperties) throws Exception {

        DelegateList result = new DelegateList();
        try {
            List<File> fileList = fileSearch.getFileList(gProperties.getAnnotationLibRootPath());
            GeniusClassLoader.setUrls(ClassListUtil.getURLArray(fileList));
        } catch (NullPointerException ne){
            throw new Exception(ErrCode.GR_I003, ne);
        } catch (Exception e) {
            throw new Exception(ErrCode.GR_I002, e);
        }
        ClassList.put(GeniusClassLoader.getUrlClassLoader(), GeniusClassLoader.getUrls());
        this.classAnalyzer(ClassList.getClassMap(), result);

        return result;
    }

    /**
     * 클래스 검색
     * @param classMap classMap
     * @param delegateList delegateList
     */
    private void classAnalyzer(Map<String, Class<?>> classMap, DelegateList delegateList){
        for(String className : classMap.keySet()){
            try {
                Class klass = Class.forName(className, false, GeniusClassLoader.getUrlClassLoader());
                this.delegateEjector(klass, delegateList);
            } catch (ClassNotFoundException e) {
                Log.console(e);
            }
        }
    }

    /**
     * 딜리게이트 추출 (Delegate 어노테이션 클래스)
     * @param klass Class
     * @param delegateList delegateList
     */
    private void delegateEjector(Class klass, DelegateList delegateList){
        if(klass.isAnnotationPresent(Delegate.class)){
            Delegate delegate = (Delegate) klass.getAnnotation(Delegate.class);

            DelegateDTO delegateDTO = new DelegateDTO(delegate.projectID());
            delegateDTO.setKlass(klass);
            delegateDTO.setServiceType(delegate.serviceType());

            Log.console("Loading Delegate * Project ID : " + delegate.projectID() + ", Service Name : "+delegateDTO.getName()+", Service Type : " + delegate.serviceType());
            this.entryEjector(delegateDTO, klass);
            delegateList.addDelegate(delegateDTO);
            Log.console("["+delegateDTO.getId() + "] "+delegateDTO.getName()+ " loaded.");
        }
    }

    /**
     * 엔트리 추출 (Entry 어노테이션 메소드)
     * @param delegateDTO DelegateDTO
     * @param klass Class
     */
    private void entryEjector(DelegateDTO delegateDTO, Class klass){
        Method[] methods = klass.getMethods();

        for(Method method : methods){
            Entry entry = method.getAnnotation(Entry.class);
            if(entry != null){
                EntryDTO entryDTO = new EntryDTO(method);
                entryDTO.setArguments(entry.arguments());
                delegateDTO.addEntry(entryDTO);
                Log.console("\t - "+entryDTO.toStringMethod());
            }
        }
    }
}
