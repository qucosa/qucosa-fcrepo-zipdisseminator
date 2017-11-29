package de.qucosa.zipdisseminator;

class InvalidMETSDocument extends Exception {
    InvalidMETSDocument(String message) {
        super(message);
    }

    InvalidMETSDocument(Throwable cause) {
        super(cause);
    }
}
