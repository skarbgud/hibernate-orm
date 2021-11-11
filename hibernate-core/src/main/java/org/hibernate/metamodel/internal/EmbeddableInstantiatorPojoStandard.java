/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

import org.hibernate.InstantiationException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Support for instantiating embeddables as POJO representation
 *
 * @author Steve Ebersole
 */
public class EmbeddableInstantiatorPojoStandard extends AbstractPojoInstantiator implements EmbeddableInstantiator {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( PojoInstantiatorImpl.class );

	private final Supplier<EmbeddableMappingType> embeddableMappingAccess;
	private final Constructor<?> constructor;

	public EmbeddableInstantiatorPojoStandard(
			Supplier<EmbeddableMappingType> embeddableMappingAccess,
			JavaType<?> javaTypeDescriptor) {
		super( javaTypeDescriptor.getJavaTypeClass() );

		this.embeddableMappingAccess = embeddableMappingAccess;
		this.constructor = resolveConstructor( javaTypeDescriptor.getJavaTypeClass() );
	}

	protected static Constructor<?> resolveConstructor(Class<?> mappedPojoClass) {
		try {
			return ReflectHelper.getDefaultConstructor( mappedPojoClass);
		}
		catch ( PropertyNotFoundException e ) {
			LOG.noDefaultConstructor( mappedPojoClass.getName() );
		}

		return null;
	}

	@Override
	public Object instantiate(Supplier<Object[]> valuesAccess, SessionFactoryImplementor sessionFactory) {
		if ( isAbstract() ) {
			throw new InstantiationException( "Cannot instantiate abstract class or interface: ", getMappedPojoClass() );
		}

		if ( constructor == null ) {
			throw new InstantiationException( "No default constructor for embeddable: ", getMappedPojoClass() );
		}

		if ( valuesAccess != null ) {
			if ( constructor.getParameterTypes().length > 0 ) {
				// constructor injection
				throw new NotYetImplementedFor6Exception( "Constructor injection for embeddables not yet implemented" );
			}
		}

		try {
			final Object instance = constructor.newInstance();
			if ( valuesAccess != null ) {
				embeddableMappingAccess.get().setPropertyValues( instance, valuesAccess.get() );
			}
			return instance;
		}
		catch ( Exception e ) {
			throw new InstantiationException( "Could not instantiate embeddable: ", getMappedPojoClass(), e );
		}
	}
}