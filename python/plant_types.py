from enum import Enum
import os
import img_helper as imghp
import numpy as np



class plant_type(Enum):
    cucumber = 0
    pomidor = 1

    def __str__(self):
        return "%s" % (self._name_)

    def getId(self):
        return self._value_


class plant_data_level(Enum):
    disease_type = 0
    disease = 1
    part = 2


class disease_leaf:

    def __init__(self, code, create_parts = False):
        # set data
        self.code = code

        # set empty children
        self.children = {}
        self.images = []

        # create parts
        if create_parts:
            self.children['leaf'] = disease_leaf(0, False)
            self.children['fruit'] = disease_leaf(0, False)
            self.children['stem'] = disease_leaf(0, False)

    def read_images(self, name, data_path, shape):
        # готовим данные
        path = os.path.join(data_path, name)

        # loading image
        self.images = imghp.read_path(path, shape)
        loaded = len(self.images)

        # load children
        for k, v in self.children.items():
            loaded += v.read_images(k, path, shape)

        # return
        return loaded


    def getImages(self, key = None, final_array = None):
        # create final array
        if final_array is None:
            final_array = []

        # add images 4 children
        for k, v in self.children.items():
            if key is None or key == k:
                final_array += v.images

        # Loop children
        for k, v in self.children.items():
            # call children
            if v.children:
                v.getImages(key, final_array)

        # возврат
        return final_array

    def getImagesCats(self, label, key = None):
        # get images
        imgs = self.getImages(key)

        # create labels
        lbls = np.zeros(len(imgs))
        lbls.fill(label)

        # return
        return imgs, lbls




