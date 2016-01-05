package br.com.estudio89.syncing.models;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;
import br.com.estudio89.syncing.DatabaseProvider;
import br.com.estudio89.syncing.injection.SyncingInjection;
import com.orm.SugarConfig;
import com.orm.SugarRecord;
import dalvik.system.DexFile;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class DatabaseReflectionUtil {
    private Context context;
    private List<Class> models = new ArrayList<Class>();

    public DatabaseReflectionUtil(Context context) {
        this.context = context;
    }

    public static DatabaseReflectionUtil getInstance() {
        return SyncingInjection.get(DatabaseReflectionUtil.class);
    }

    public List<Class> getDomainClasses() {
        if (models.size() > 0) {
            return models;
        }

        models = new ArrayList<Class>();
        try {
            for (String className : getAllClasses()) {
                Class domainClass = getDomainClass(className);
                if (domainClass != null) models.add(domainClass);
            }
        } catch (IOException e) {
            Log.e("DatabaseReflectionUtil", e.getMessage());
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("DatabaseReflectionUtil", e.getMessage());
        }

        return models;
    }


    private Class getDomainClass(String className) {
        Class<?> discoveredClass = null;
        try {
            discoveredClass = Class.forName(className, true, context.getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            Log.e("DatabaseReflectionUtil", e.getMessage());
        }

        if ((discoveredClass != null) &&
                ((SugarRecord.class.isAssignableFrom(discoveredClass) &&
                        !SugarRecord.class.equals(discoveredClass))) &&
                !Modifier.isAbstract(discoveredClass.getModifiers())) {

            Log.i("DatabaseReflectionUtil", "domain class : " + discoveredClass.getSimpleName());
            return discoveredClass;

        } else {
            return null;
        }
    }


    private List<String> getAllClasses() throws PackageManager.NameNotFoundException, IOException {
        String packageName = SugarConfig.getDomainPackageName(context);
        String path = getSourcePath();
        List<String> classNames = new ArrayList<String>();
        DexFile dexfile = null;
        try {
            dexfile = new DexFile(path);
            Enumeration<String> dexEntries = dexfile.entries();
            while (dexEntries.hasMoreElements()) {
                String className = dexEntries.nextElement();
                if (className.startsWith(packageName)) classNames.add(className);
            }
        } catch (NullPointerException e) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> urls = classLoader.getResources("");
            while (urls.hasMoreElements()) {
                List<String> fileNames = new ArrayList<String>();
                String classDirectoryName = urls.nextElement().getFile();
                if (classDirectoryName.contains("bin") || classDirectoryName.contains("classes")) {
                    File classDirectory = new File(classDirectoryName);
                    for (File filePath : classDirectory.listFiles()) {
                        populateFiles(filePath, fileNames, "");
                    }
                    for (String fileName : fileNames) {
                        if (fileName.startsWith(packageName)) classNames.add(fileName);
                    }
                }
            }
        } finally {
            if (null != dexfile) dexfile.close();
        }
        return classNames;
    }

    private void populateFiles(File path, List<String> fileNames, String parent) {
        if (path.isDirectory()) {
            for (File newPath : path.listFiles()) {
                if ("".equals(parent)) {
                    populateFiles(newPath, fileNames, path.getName());
                } else {
                    populateFiles(newPath, fileNames, parent + "." + path.getName());
                }
            }
        } else {
            String pathName = path.getName();
            String classSuffix = ".class";
            pathName = pathName.endsWith(classSuffix) ?
                    pathName.substring(0, pathName.length() - classSuffix.length()) : pathName;
            if ("".equals(parent)) {
                fileNames.add(pathName);
            } else {
                fileNames.add(parent + "." + pathName);
            }
        }
    }

    private String getSourcePath() throws PackageManager.NameNotFoundException {
        return context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).sourceDir;
    }

    public void createDB() {
        new AsyncDBCreation().execute();
    }

    public void eraseData(){
        for (Class Model:models) {
            SugarRecord.deleteAll(Model);
        }
    }

    public class AsyncDBCreation extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            // Quickstarting tables creation
            ((DatabaseProvider) context).getApplicationDatabase();

            for (Class<? extends SugarRecord> klass:getDomainClasses()) {
                try {
                    SugarRecord sr = klass.newInstance();
                    sr.getTableFields();
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        }
    }
}
