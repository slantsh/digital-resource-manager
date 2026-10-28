package model;

/**
 * Simple enum representing the supported types of resources.
 * This ensures only valid types (PDF, IMAGE, VIDEO, LINK) are used.
 */
public enum ResourceType {
    PDF,
    IMAGE,
    VIDEO,
    LINK;

    /**
     * Automatically determine the ResourceType from a file name, path, or URL.
     */
    public static ResourceType fromFileName(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.trim().isEmpty()) {
            return PDF;
        }
        String trimmed = pathOrUrl.trim().toLowerCase();

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("www.")) {
            return LINK;
        }

        if (trimmed.endsWith(".pdf")) {
            return PDF;
        }

        if (trimmed.endsWith(".png") || trimmed.endsWith(".jpg") || trimmed.endsWith(".jpeg")
                || trimmed.endsWith(".gif") || trimmed.endsWith(".bmp") || trimmed.endsWith(".webp")
                || trimmed.endsWith(".svg") || trimmed.endsWith(".ico") || trimmed.endsWith(".tiff")) {
            return IMAGE;
        }

        if (trimmed.endsWith(".mp4") || trimmed.endsWith(".mkv") || trimmed.endsWith(".avi")
                || trimmed.endsWith(".mov") || trimmed.endsWith(".wmv") || trimmed.endsWith(".webm")
                || trimmed.endsWith(".flv") || trimmed.endsWith(".m4v") || trimmed.endsWith(".3gp")) {
            return VIDEO;
        }

        if (trimmed.endsWith(".html") || trimmed.endsWith(".htm") || trimmed.endsWith(".url")) {
            return LINK;
        }

        // Default fallback
        return PDF;
    }
}

