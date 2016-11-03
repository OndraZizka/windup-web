import {BaseModel} from './BaseModel';

/**
 * Mapping between discriminator and model classes.
 */
export class DiscriminatorMapping
{
    static mapping: { [key: string]: typeof BaseModel } = {};

    public static getModelClassByDiscriminator(discriminator: string): typeof BaseModel
    {
        return this.mapping[discriminator];
    }

    public static addModelClass(clazz: typeof BaseModel) {
        this.mapping[clazz.discriminator] = clazz;
    }

    public static getDiscriminatorByModelClass(clazz: typeof BaseModel)
    {
        // It should be in the class' static data.
        if(clazz.discriminator)
            return clazz.discriminator;

        return null;
    }


    public toString() : string {
        let mapping_ = Object.getPrototypeOf(this).constructor.mapping;
        return `${Object.getPrototypeOf(this).constructor}{${Object.getOwnPropertyNames(mapping_).length}}`;
    }
}

export function getParentClass(clazz){
    return parent = Object.getPrototypeOf(Object.getPrototypeOf(new clazz())).constructor;
}

