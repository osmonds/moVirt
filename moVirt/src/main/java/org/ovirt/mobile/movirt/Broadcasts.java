package org.ovirt.mobile.movirt;

import static org.ovirt.mobile.movirt.Constants.APP_PACKAGE_DOT;

public interface Broadcasts {
    String ERROR_MESSAGE = APP_PACKAGE_DOT + "ERROR_MESSAGE";
    String REST_CA_FAILURE = APP_PACKAGE_DOT + "REST_CA_FAILURE";
    String IN_SYNC = APP_PACKAGE_DOT + "IN_SYNC";
    String EVENTS_IN_SYNC = APP_PACKAGE_DOT + "EVENTS_IN_SYNC";
    String NO_CONNECTION_SPECIFIED = APP_PACKAGE_DOT + "NO_CONNECTION_SPECIFIED";
    String IN_USER_LOGIN = APP_PACKAGE_DOT + "IN_USER_LOGIN";
    String DOWNLOADING_CERTIFICATE = APP_PACKAGE_DOT + "DOWNLOADING_CERTIFICATE";

    interface Extras {
        String ERROR_REASON = APP_PACKAGE_DOT + "ERROR_REASON";
        String REPEATED_MINOR_ERROR = APP_PACKAGE_DOT + "REPEATED_MINOR_ERROR";
        String SYNCING = APP_PACKAGE_DOT + "SYNCING";
        String MESSAGE = APP_PACKAGE_DOT + "MESSAGE";
        String IN_PROGRESS = APP_PACKAGE_DOT + "IN_PROGRESS";
    }
}
