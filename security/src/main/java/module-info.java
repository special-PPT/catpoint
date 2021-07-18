module com.udacity.catpoint.security{
    requires com.udacity.catpoint.image;
    requires miglayout;
    requires java.desktop;
    requires com.google.common;
    requires java.prefs;
    requires com.google.gson;
    opens com.udacity.catpoint.security.data to com.google.gson;
}