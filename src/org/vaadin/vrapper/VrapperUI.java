package org.vaadin.vrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.Type;
import org.vaadin.vrapper.model.WidgetConfiguration;
import org.vaadin.vrapper.model.reflect.ClassType;
import org.vaadin.vrapper.model.reflect.ClasspathResolver;
import org.vaadin.vrapper.model.reflect.ListResolver;
import org.vaadin.vrapper.model.reflect.Resolver;
import org.vaadin.vrapper.model.reflect.TypeSource;
import org.vaadin.vrapper.model.reflect.ZipResolver;

import com.vaadin.annotations.Theme;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.NativeSelect;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.FailedEvent;
import com.vaadin.ui.Upload.FailedListener;
import com.vaadin.ui.Upload.FinishedEvent;
import com.vaadin.ui.Upload.FinishedListener;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.VerticalLayout;

/*
 * TODO:
 * - Prettier overall structure (tabs?)
 * - Widget class selector UI
 *   
 * - Use TabIndexState or AbstractFieldState when appropriate and filter out methods automatically handled, e.g. setTabIndex and setFocus
 * - Filter out more Widget methods that are handled by Vaadin, e.g. setEnabled
 * 
 * - Support generics (especially to avoid defining e.g. Set<?> as types in RPC or state)
 * 
 * - Multi-value setters using a bean instead of multiple fields in the state
 * - Option to generate EventRouter code for server-side event handling
 * 
 * - Jar download, requires integrating a compiler
 *  
 */
@SuppressWarnings("serial")
@Theme("vrapper")
public class VrapperUI extends UI {

    private static final String clientJarName = "vaadin-client-7.1.0.jar";
    private static final File clientJarFile;
    static {
        File file = new File("src", clientJarName);
        if (file.exists()) {
            clientJarFile = file;
        } else {
            clientJarFile = copyClientJar(clientJarName);
        }
    }

    private ZipFile clientJar;

    @Override
    protected void init(VaadinRequest request) {
        try {
            clientJar = new ZipFile(clientJarFile);
            showJarUpload();

            // Resolver resolver = createResolver(null);

            // showTypeSelector(zipFile);

            // Type widgetType = Type
            // .getObjectType("com/google/gwt/user/client/ui/HTML");
            // showWidgetConfigurator(widgetType);

        } catch (Exception e) {
            setContent(new Label(e.getMessage()));
            e.printStackTrace();
        }
    }

    private static File copyClientJar(String clientJarName) {
        FileOutputStream output = null;
        InputStream input = VrapperUI.class.getClassLoader()
                .getResourceAsStream(clientJarName);
        try {
            if (input == null) {
                throw new RuntimeException(clientJarName + " not found");
            }
            File tempFile = File.createTempFile("vrapper", clientJarName);
            tempFile.deleteOnExit();
            output = new FileOutputStream(tempFile);

            IOUtils.copy(input, output);

            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }
    }

    private Resolver createResolver(Resolver ownResolver) throws IOException {

        ArrayList<Resolver> resolvers = new ArrayList<Resolver>();

        if (ownResolver != null) {
            resolvers.add(ownResolver);
        }
        resolvers.add(new ZipResolver(clientJar));
        resolvers.add(new ClasspathResolver());

        final ListResolver listResolver = new ListResolver(resolvers);
        addDetachListener(new DetachListener() {
            @Override
            public void detach(DetachEvent event) {
                try {
                    listResolver.close();
                } catch (IOException e) {
                    System.err
                            .println("Couldn't close resolver - might leak resources");
                    e.printStackTrace();
                }
            }
        });

        return listResolver;
    }

    private File currentUploadFile = null;

    private void showJarUpload() {
        final Upload upload = new Upload();
        upload.setCaption("Upload jar with widget");

        upload.setReceiver(new Receiver() {
            @Override
            public OutputStream receiveUpload(String filename, String mimeType) {
                if (currentUploadFile != null) {
                    Notification.show("There is already an upload in progress");
                    return null;
                }

                try {
                    currentUploadFile = File.createTempFile("vrapperUpload",
                            filename);
                    currentUploadFile.deleteOnExit();

                    System.out.println(currentUploadFile.getAbsolutePath());

                    return new FileOutputStream(currentUploadFile);
                } catch (IOException e) {
                    Notification.show("Could not create temporary file");
                    return null;
                }
            }
        });

        upload.addFinishedListener(new FinishedListener() {
            @Override
            public void uploadFinished(FinishedEvent event) {
                File uploadedFile = currentUploadFile;
                currentUploadFile = null;
                handleUploadedFile(uploadedFile);
            }
        });

        upload.addFailedListener(new FailedListener() {
            @Override
            public void uploadFailed(FailedEvent event) {
                Notification.show("Upload failed, please try again");
                currentUploadFile = null;
            }
        });

        Button clientJarButton = new Button("Just use vaadin-client.jar",
                new Button.ClickListener() {
                    @Override
                    public void buttonClick(ClickEvent event) {
                        try {
                            showTypeSelector(createResolver(null), clientJar);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
        Button htmlWidgetButton = new Button("Just use the HTML widget",
                new Button.ClickListener() {
                    @Override
                    public void buttonClick(ClickEvent event) {
                        try {
                            showWidgetConfigurator(
                                    createResolver(null),
                                    Type.getObjectType("com/google/gwt/user/client/ui/HTML"));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
        VerticalLayout layout = new VerticalLayout(upload, clientJarButton,
                htmlWidgetButton);
        layout.setSpacing(true);
        layout.setMargin(true);
        setContent(layout);
    }

    private void handleUploadedFile(final File uploadedFile) {
        try {
            ZipFile uploadedZip = new ZipFile(uploadedFile);
            ZipResolver zipResolver = new ZipResolver(uploadedZip) {
                @Override
                public void close() throws IOException {
                    System.out.println("Closing and deleting temp file "
                            + uploadedFile);
                    super.close();
                    uploadedFile.delete();
                }
            };
            Resolver resolver = createResolver(zipResolver);
            showTypeSelector(resolver, uploadedZip);
        } catch (IOException e) {
            Notification.show(e.getLocalizedMessage(),
                    Notification.Type.ERROR_MESSAGE);
        }
    }

    private void showTypeSelector(final Resolver resolver, ZipFile zipFile) {
        TypeSource typeSource = new TypeSource(resolver);

        List<Type> widgetTypes = new ArrayList<Type>();

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            String name = zipEntry.getName();
            if (name.endsWith(".class")) {
                String internalName = name.substring(0, name.length() - 6);
                Type type = Type.getObjectType(internalName);
                ClassType classType = (ClassType) typeSource
                        .getTypeByInternalName(type.getDescriptor());
                try {
                    if (classType.isPublic() && !classType.isAbstract()
                            && isWidgetType(classType)) {
                        widgetTypes.add(type);
                    }
                } catch (Exception e) {
                    System.out.println("Ignoring " + classType.getClassName());
                }
            }
        }

        final NativeSelect typeSelector = new NativeSelect("Select widget",
                widgetTypes);
        typeSelector.setImmediate(true);
        typeSelector.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                Type selectedType = (Type) typeSelector.getValue();
                if (selectedType != null) {
                    showWidgetConfigurator(resolver, selectedType);
                }
            }
        });

        setContent(typeSelector);
    }

    private boolean isWidgetType(ClassType type) {
        if (type.getClassName().equals("com.google.gwt.user.client.ui.Widget")) {
            return true;
        } else {
            try {
                ClassType superType = type.getSuperType();
                if (superType == null) {
                    return false;
                } else {
                    return isWidgetType(superType);
                }
            } catch (Exception e) {
                System.out.println("Could not find superclass for "
                        + type.getClassName());
                return false;
            }
        }
    }

    private void showWidgetConfigurator(Resolver resolver, Type widgetType) {
        TypeSource typeSource = new TypeSource(resolver);

        ClassType widgetClass = (ClassType) typeSource
                .getTypeByInternalName(widgetType.getDescriptor());

        WidgetConfiguration configuration = new WidgetConfiguration(widgetClass);

        WidgetConfigurator widgetConfigurator = new WidgetConfigurator(
                configuration);
        setContent(widgetConfigurator);
    }

    @Override
    public void detach() {
        super.detach();
        System.out.println("Need to figure out how to close the resolver");
    }

}