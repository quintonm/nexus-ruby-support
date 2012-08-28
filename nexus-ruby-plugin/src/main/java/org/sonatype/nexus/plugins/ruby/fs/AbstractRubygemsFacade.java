package org.sonatype.nexus.plugins.ruby.fs;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.sonatype.nexus.plugins.ruby.RubyGateway;
import org.sonatype.nexus.plugins.ruby.RubyRepository;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;

public abstract class AbstractRubygemsFacade implements RubygemsFacade {

    protected final RubyGateway gateway;
    protected final RubyRepository repository;
    
    public AbstractRubygemsFacade( RubyGateway gateway, RubyRepository repository )
    {
        this.gateway = gateway;
        this.repository = repository;
    }
    
    @Override
    public void addGem( RubyLocalRepositoryStorage storage, StorageFileItem gem ) 
            throws UnsupportedStorageOperationException, LocalStorageException
    {
        throw new UnsupportedStorageOperationException( "can not add gems through this repository: " + repository );
    }
    
    @Override
    public void removeGem( RubyLocalRepositoryStorage storage, StorageFileItem gem )
            throws UnsupportedStorageOperationException, LocalStorageException
    {
        throw new UnsupportedStorageOperationException( "can not remove gems through this repository: " + repository );
    }

    @Override
    public void mergeSpecsIndex( RubyLocalRepositoryStorage storage,
            SpecsIndexType type, StorageItem localSpecs, List<StorageItem> specsItems )
            throws UnsupportedStorageOperationException, LocalStorageException, IOException {
        throw new UnsupportedStorageOperationException( "can not merge specs-indeces for this repository: " + repository );
    }

    protected InputStream toInputStream(StorageFileItem item) throws LocalStorageException {
        try
        {
            
            return item.getInputStream();
        
        }
        catch (IOException e) {
            throw new LocalStorageException( "error getting stream to: " + item, e );
        }
    }
}