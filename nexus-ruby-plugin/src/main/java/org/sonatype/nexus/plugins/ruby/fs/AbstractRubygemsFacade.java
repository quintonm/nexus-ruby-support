package org.sonatype.nexus.plugins.ruby.fs;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.sonatype.nexus.plugins.ruby.RubyRepository;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.ruby.BundlerDependencies;
import org.sonatype.nexus.ruby.RubygemsGateway;
import org.sonatype.nexus.ruby.SpecsIndexType;

public abstract class AbstractRubygemsFacade implements RubygemsFacade {

    protected final RubygemsGateway gateway;
    protected final RubyRepository repository;
    
    public AbstractRubygemsFacade( RubygemsGateway gateway, RubyRepository repository )
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
    public boolean removeGem( RubyLocalRepositoryStorage storage, StorageFileItem gem )
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

    protected InputStream toGZIPInputStream( StorageFileItem item ) throws LocalStorageException {
        try
        {

            if ( item != null )
            {
                return new GZIPInputStream( item.getInputStream() );
            }
            else
            {
                return null;
            }
        
        }
        catch ( IOException e ) {
            throw new LocalStorageException( "error getting stream to: " + item, e );
        }
    }

    protected InputStream toInputStream( StorageFileItem item ) throws LocalStorageException {
        try
        {

            if ( item != null )
            {
                return item.getInputStream();
            }
            else
            {
                return null;
            }
        
        }
        catch ( IOException e ) {
            throw new LocalStorageException( "error getting stream to: " + item, e );
        }
    }

    protected void storeSpecsIndex(RubyRepository repository, RubyLocalRepositoryStorage storage, SpecsIndexType type,
            InputStream newSpecsIndex) throws LocalStorageException,
            UnsupportedStorageOperationException
    {
        if ( newSpecsIndex != null )
        {
            storage.storeSpecsIndex( repository, type, newSpecsIndex );
        }
    }

    @Override
    public RubygemFile deletableFile( String path )
    {
        return RubygemFile.fromFilename( path );
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public BundlerDependencies bundlerDependencies()
            throws AccessDeniedException, ItemNotFoundException, IllegalOperationException, 
                    org.sonatype.nexus.proxy.StorageException
    {
        StorageFileItem specs = 
                (StorageFileItem) repository.retrieveItem( new ResourceStoreRequest( SpecsIndexType.RELEASE.filepathGzipped() ) );
        StorageFileItem prereleasedSpecs = 
                (StorageFileItem) repository.retrieveItem( new ResourceStoreRequest( SpecsIndexType.PRERELEASE.filepathGzipped() ) );

        return gateway.newBundlerDependencies( toGZIPInputStream( specs ), specs.getModified(), 
                toGZIPInputStream( prereleasedSpecs ), prereleasedSpecs.getModified() );
    }

    @Override
    @SuppressWarnings("deprecation")
    public StorageFileItem[] prepareDependencies( BundlerDependencies bundler, String... gemnames )
            throws ItemNotFoundException, AccessDeniedException, IllegalOperationException, 
                    org.sonatype.nexus.proxy.StorageException
    {
        if ( gemnames.length == 1 && gemnames[0].length() == 0 )
        {
            gemnames = new String[ 0 ];
        }
        StorageFileItem[] result = new StorageFileItem[ gemnames.length ];
        int index = 0;
        for( String gemname: gemnames )
        {
            doPrepareDependencies( bundler, gemname );
            result[ index++ ] = repository.retrieveDependenciesItem( gemname );
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    private void doPrepareDependencies( BundlerDependencies bundler, String gemname )
                throws ItemNotFoundException, AccessDeniedException, IllegalOperationException, 
                        org.sonatype.nexus.proxy.StorageException
        {
        StorageFileItem dependencies = repository.retrieveDependenciesItem( gemname );
        try {

            String[] missing = bundler.add(gemname, 
                    dependencies == null ? null : dependencies.getInputStream());
            if ( missing.length > 0 || dependencies == null )
            {
                InputStream[] missingSpecs = new InputStream[missing.length];
                int index = 0;
                for (String version : missing)
                {
                    StorageFileItem item = 
                            repository.retrieveGemspec( gemname + "-" + version );
                    missingSpecs[ index++ ] = item.getInputStream();
                }
                String json = bundler.update( gemname,
                        dependencies == null ? null : dependencies.getInputStream(), missingSpecs );

                repository.storeDependencies( gemname, json );
            }
        } 
        catch ( IOException e )
        {
            throw new ItemNotFoundException( dependencies.getResourceStoreRequest(), e );
        }
        catch ( UnsupportedStorageOperationException e )
        {
            throw new RuntimeException("BUG: must be able to store data" );
        }
    }
}