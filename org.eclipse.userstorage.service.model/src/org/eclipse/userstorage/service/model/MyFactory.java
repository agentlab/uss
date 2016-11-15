package org.eclipse.userstorage.service.model;

import org.eclipse.userstorage.service.model.impl.BlobInterfaceRequestImpl;

public class MyFactory implements IFactory {

	@Override
	public Object create(Class<?> clazz) {
        return new BlobInterfaceRequestImpl();
	}
}
