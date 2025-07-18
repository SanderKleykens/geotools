:Author: Devon Tucker
:Thanks: geotools-user list
:Version: |release|
:License: Create Commons with attribution

.. _JetBrains: https://www.jetbrains.com/idea/

IntelliJ Quickstart
===================
This guide will help you setup the IntelliJ IDE in order to work with GeoTools and follow along with the rest of the
GeoTools tutorial.

Pre-Requisites
--------------
This guide assumes the following:

* You have the latest JDK installed (17 at the time this article was written) installed. If not the `Eclipse Quickstart <./eclipse.html>`_
  provides instructions on how to do this.
* You have IntelliJ installed. This article targets IntelliJ CE 2016; however, previous versions at least as far back as
  13 should work fine. Ultimate versions should also work fine. IntelliJ can be downloaded from JetBrains_ and generally
  works out of the box on common operating systems.
  
Create a New Project
--------------------
To start with we'll create a new project using the Maven quickstart archetype.

#. Choose :menuselection:`File -> New Project` from the menu. In the New Project dialog choose Maven project, ensure *Create from archetype* is selected,
   and choose the *org.apache.maven.archetypes:maven-archetype-quickstart* archetype. Press *Next*
   
   .. image:: images/intellij/new_project_screen.png
      :scale: 65 %
      :align: center
      
#. The next screen asks us for basic identifying information for our project:
   
   * GroupId: ``org.geotools``
   * ArtifactId: ``tutorial``
   * Version: ``1.0-SNAPSHOT``
   
   .. image:: images/intellij/new_project2.png
      :scale: 65 %
      :align: center
      
#. Hit next. The following screen we should be able to leave with the defaults. For our purposes IntelliJ's bundled Maven should be
   fine, unless the version is lower than 3, in which case you should consider using a new external version. 
   
   .. image:: images/intellij/new_project3.png
      :scale: 65 %
      :align: center
   
#. Hit next. Give the project a name (this name is only used internally by IntelliJ), tutorial should work for our
   purposes. You can change the project location to suit your needs and hopefully leave *More Settings* as their defaults (recommended)
   
   .. image:: images/intellij/new_project4.png
      :scale: 65 %
      :align: center
   
#. Hit finish and our new project will be created. IntelliJ will show us our newly created Maven file and do an initial Maven build 
   (let this finish before attempting the next steps, it shouldn't take long). IntelliJ should also ask if you want to enable *Auto Import*
   for Maven dependencies. Let's turn that on for the purposes of this tutorial, it will automatically detect changes we make to 
   our POM file and automatically import them.
   
   .. image:: images/intellij/auto_import.png
      :align: center
   
IntelliJ has created an empty App.java with a simple *Hello World!* along with a **JUnit** test case. You can run *App* or *AppTest*
by right clicking on them in the Project Explorer and choosing *Run* from the context menu.

.. image:: images/intellij/run_menu.png
   :align: center


Adding Jars to Your Project
---------------------------

.. sidebar:: Lab
   
   If you're following along with this tutorial a pre-loaded Maven repository may have been provided. We can use *Offline Mode*
   to ensure Maven doesn't try to download any dependencies.
   
   To turn on Offline Mode:
   
   #. Open the *Settings*. On OS X this is :menuselection:`IntelliJ -> Preferences`, on other OSes it's under :menuselection:`File -> Settings`
   #. Choose :menuselection:`Build, Execution, Deployment -> Build Tools -> Maven`
   #. Check the *Work Offline* option
   
The ``pom.xml`` file describes the structure, configuration, dependencies and many other facets of your project. We are going to focus
on the dependencies needed for your project.

When downloading jars Maven makes use of a "local repository" to store copies if the dependencies it downloads.

  ==================  ========================================================
     PLATFORM           LOCAL REPOSITORY
  ==================  ========================================================
     Windows XP:      :file:`C:\\Documents and Settings\\You\\.m2\\repository`
     Windows:         :file:`C:\\Users\\You\\.m2\repository`
     Linux and Mac:   :file:`~/.m2/repository`
  ==================  ========================================================
  
Maven downloads jars from public repositories on the internet where projects such as GeoTools publish their work.

#. Open up the :file:`pom.xml` file at the root of the project. You can see some of the information we entered through the wizard
   earlier.
   
#. We're going to add several things to this file. First, at the top of the file after ``moduleVersion`` we want to add a 
   properties element defining the version of GeoTools we wish to use. This workbook was written for |release| 
   although you may wish to try a different version.
   
   For production a stable release of |branch| should be used for `geotools.version`:
    
   .. literalinclude:: /../../tutorials/quickstart/pom.xml
        :language: xml
        :start-at: <properties>
        :end-at: </properties>
   
   To make use of a nightly build set the `geotools.version` property to |branch|-SNAPSHOT .

#. We use the GeoTools Bill of Materials (BOM) to manage dependency versions. This ensures that all GeoTools modules use compatible versions:

   .. literalinclude:: /../../tutorials/quickstart/pom.xml
        :language: xml
        :start-at: <dependencyManagement>
        :end-at: </dependencyManagement>

   The BOM (Bill of Materials) pattern centralizes version management. By importing the ``gt-bom``, we don't need to specify version numbers for individual GeoTools modules.
        
#. We add dependencies to GeoTools modules. Note that we don't specify version numbers since these are managed by the BOM:
   
   .. literalinclude:: /../../tutorials/quickstart/pom.xml
        :language: xml
        :start-after: </dependencyManagement>
        :end-at: </dependencies>
    
#. Finally we need to list the external *repositories* where maven can download GeoTools and
   other required jars from.

   .. literalinclude:: /../../tutorials/quickstart/pom.xml
        :language: xml
        :start-at: <repositories>
        :end-at: </repositories>

   .. note:: Note the snapshot repository above is only required if you are using a nightly build (such as |branch|-SNAPSHOT)

#. GeoTools requires Java 17, you need to tell Maven to use the 17 source level

   .. literalinclude::  /../../tutorials/quickstart/pom.xml
      :language: xml
      :start-after: </repositories>
      :end-at: </build>

#. Here is what the completed :file:`pom.xml` looks like:

   .. literalinclude:: /../../tutorials/quickstart/pom.xml
        :language: xml
        :end-before: <profiles>
        :append: </project>
   
   * Recommend cutting and pasting the above to avoid mistakes when typing, using choose :menuselection:`Code -> Reformat Code` to help fix indentation

   * You may also download :download:`pom.xml </../../tutorials/quickstart/pom.xml>`, if this opens in your browser use :command:`Save As` to save to disk.
   
     The download has an optional quality assurance profile you can safely ignore. 

Tips:

* If Maven isn't downloading dependencies automatically for some reason (maybe *Auto-Import* is turned off)
  you can manually download dependencies by right-clicking on your project and choosing :menuselection:`Maven -> Reimport`.
* If you'd like to download the Javadoc for your dependencies you can again go to the Maven context menu and choose
  *Download Documentation*
  
Quickstart Application
----------------------
Now that our environment is set up we can put together a simple Quickstart. This example will display a
shapefile on the screen.

#. Let's create a class called `Quickstart` in the package ``org.geotools.tutorial.quickstart``. IntelliJ can 
   create both the package and the class for us in one shot; right click on the ``org.geootools`` package in the Project panel
   and in the context menu choose :menuselection:`New -> Java Class`.
   
   .. image:: images/intellij/new_class_menu.png
      :align: center
      
   .. image:: images/intellij/new_class_dialog.png
      :align: center
      
#. Fill in the following code :file:`Quickstart.java`:
  
   .. literalinclude:: /../../tutorials/quickstart/src/main/java/org/geotools/tutorial/quickstart/Quickstart.java
      :language: java
      
   * You may find cutting and pasting from the documentation to be easier then typing.
   
   * You may also download :download:`Quickstart.java </../../tutorials/quickstart/src/main/java/org/geotools/tutorial/quickstart/Quickstart.java>`
      
#. We need to download some sample data to work with. The http://www.naturalearthdata.com/ project
   is a great project supported by the North American Cartographic Information Society.  Head to the link below and download some cultural vectors. You can use the 'Download all 50m cultural themes' at top.

   * `1:50m Cultural Vectors <http://www.naturalearthdata.com/downloads/50m-cultural-vectors/>`_

   Please unzip the above data into a location you can find easily such as the desktop.

#. Run the application to open a file chooser. Choose a shapefile from the example data set.

   .. image:: images/QuickstartOpen.png
      :scale: 60
 
#. The application will connect to your shapefile, produce a map content, and display the shapefile.

   .. image:: images/QuickstartMap.png
      :scale: 60
 
#. A couple of things to note about the code example:
 
   * The shapefile is not loaded into memory - instead it is read from disk each and every time it is needed
     This approach allows you to work with data sets larger than available memory.
    
   * We are using a very basic display style here that just shows feature outlines. In the examples that follow we will see how to specify more sophisticated styles.
  
Things to Try
-------------

.. include:: try.txt

* When cutting and pasting GeoTools examples often the code compile due to missing imports.

  IntelliJ should prompt to import the missing class immediately. Press Alt-Enter (^-Enter on OS X) to bring up a dialog to import any missing classes.
