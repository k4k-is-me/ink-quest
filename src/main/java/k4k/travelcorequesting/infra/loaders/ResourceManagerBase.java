package k4k.travelcorequesting.infra.loaders;

import k4k.travelcorequesting.TravelcoreQuesting;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStream;

public abstract class ResourceManagerBase implements SimpleSynchronousResourceReloadListener {

    abstract public String getStartingPath();

    abstract public String getFileExtension();

    abstract public void setup();

    abstract public void process(InputStream stream, Identifier resourceId, Identifier strippedResourceId);

    @Override
    public void reload(ResourceManager manager) {
        this.setup();

        var resources = manager.findResources(this.getStartingPath(), this::matchesExtension);

        for (var resouceEntry : resources.entrySet()) {
            TravelcoreQuesting.LOGGER.info("Loading resource {}", resouceEntry.getKey());

            try (InputStream stream = resouceEntry.getValue().getInputStream()) {
                this.process(
                    stream,
                    resouceEntry.getKey(),
                    this.stripResourceId(resouceEntry.getKey())
                );
            } catch (Exception e) {
                TravelcoreQuesting.LOGGER.error("Error occurred while loading resource json {}", resouceEntry.getKey().toString(), e);
            }
        }
    }

    private String getDottedExtension() {
        var ext = this.getFileExtension();

        if (!ext.startsWith("."))
            return ".".concat(ext);

        return ext;
    }

    private boolean matchesExtension(Identifier resourceId) {
        return resourceId.getPath()
            .endsWith(this.getDottedExtension());
    }

    private Identifier stripResourceId(Identifier resourceId) {
        var path = resourceId.getPath();
        var base = getStartingPath();
        var ext = getDottedExtension();

        if (path.startsWith(base + "/"))
            path = path.substring(base.length() + 1);

        if (path.endsWith(ext))
            path = path.substring(0, path.length() - ext.length());

        return resourceId.withPath(path);
    }

}
