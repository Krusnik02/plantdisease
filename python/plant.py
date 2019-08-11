import img_helper as imghp
import numpy as np
import model_helper
import os
import plant_types as pt
from plant_types import plant_type
import random as rnd




class plant:
    # static
    parts = {'leaf': 0, 'fruit': 1, 'stem': 2}

    # constructor
    def __init__(self, tp_ky, shape):
        # store data
        self.type_key = tp_ky
        self.shape = shape

        # models n data
        self.models = {}
        self.disease_tree = pt.disease_leaf(-1)


        # disease type good
        self.disease_tree.children['good'] = pt.disease_leaf(0)
        self.disease_tree.children['good'].children['good'] = pt.disease_leaf(0, True)

        # add disease types && diseases
        if self.type_key == plant_type.pomidor:
            # add disorders
            self.disease_tree.children['disorders'] = pt.disease_leaf(1)
            self.disease_tree.children['disorders'].children['Blossom end rot'] = pt.disease_leaf(1, True)
            self.disease_tree.children['disorders'].children['Cracked fruits'] = pt.disease_leaf(2, True)
            self.disease_tree.children['disorders'].children['Sunscald'] = pt.disease_leaf(3, True)
            self.disease_tree.children['disorders'].children['Catfacing'] = pt.disease_leaf(4, True)
            self.disease_tree.children['disorders'].children['Tomato nose'] = pt.disease_leaf(5, True)
            self.disease_tree.children['disorders'].children['GreenYellow sholders'] = pt.disease_leaf(6, True)

            # add bacterial
            self.disease_tree.children['bacterial'] = pt.disease_leaf(2)
            self.disease_tree.children['bacterial'].children['Bacterial wilt'] = pt.disease_leaf(7, True)
            self.disease_tree.children['bacterial'].children['Canker'] = pt.disease_leaf(8, True)
            self.disease_tree.children['bacterial'].children['Bacterial spot'] = pt.disease_leaf(9, True)
            self.disease_tree.children['bacterial'].children['Speck'] = pt.disease_leaf(10, True)
            self.disease_tree.children['bacterial'].children['Pith necrosis'] = pt.disease_leaf(11, True)

            # add fungal
            self.disease_tree.children['fungal'] = pt.disease_leaf(3)
            self.disease_tree.children['fungal'].children['Early blight'] = pt.disease_leaf(12, True)
            self.disease_tree.children['fungal'].children['Gray mold'] = pt.disease_leaf(13, True)
            self.disease_tree.children['fungal'].children['Late blight'] = pt.disease_leaf(14, True)
            self.disease_tree.children['fungal'].children['Septoria leaf spot'] = pt.disease_leaf(15, True)
            self.disease_tree.children['fungal'].children['Verticillium wilt'] = pt.disease_leaf(16, True)
            self.disease_tree.children['fungal'].children['White mold'] = pt.disease_leaf(17, True)

            # add virus
            self.disease_tree.children['viruses'] = pt.disease_leaf(3)
            self.disease_tree.children['viruses'].children['Tobacco mosaic'] = pt.disease_leaf(18, True)


    # get name
    def get_name(self):
        return str(self.type_key)


    # read data
    def read_images(self, data_path):
        # сообщаем
        print('Загружаем %s:' % self.get_name())

        # грузим
        loaded = self.disease_tree.read_images(self.get_name(), data_path, self.shape)

        # отчитываемся
        print('%s всего: - %i' % (self.get_name(), loaded))


    # получаем данные для классификации растений
    def get_train_data_all(self):
        # грузим всё
        imgs = self.disease_tree.getImages()

        # готовим значения
        labels = np.zeros(len(imgs))
        labels.fill(self.type_key.getId())

        # возвращаем нули как значения
        return imgs, labels

    # получаем данные для классификации растений
    def get_train_data_parts(self):
        # цикл по всем типам болезней - тока чтобы не выводить на верх класс ограничицца дикшинари
        imgs = []
        labels = np.arange(0)

        self.disease_tree.getImages()

        # loop parts
        for part_key, part_id in self.parts.items():
            # читаем изображения
            img_new = self.disease_tree.getImages(part_key)
            labels_new = np.zeros(len(img_new))
            labels_new.fill(part_id)

            # добавляем
            imgs += img_new
            labels = np.append(labels, labels_new)

        # возврат
        return np.asarray(imgs), labels



    # get data to train
    def get_train_data_disease_parts(self, part):
        # цикл по всем типам болезней - тока чтобы не выводить на верх класс ограничицца дикшинари
        imgs = []
        labels = np.arange(0)

        # loop disease types
        for dtk, dtv in self.disease_tree.children.items():
            # loop diseases
            for disease_key, disease_value in dtv.children.items():
                img_new = disease_value.getImages(part)
                labels_new = np.zeros(len(img_new))
                labels_new.fill(disease_value.code)

                # добавляем
                imgs += img_new
                labels = np.append(labels, labels_new)

        # возврат
        return imgs, labels


    # get data to train
    def get_train_data_diseases(self):
        # цикл по всем типам болезней - тока чтобы не выводить на верх класс ограничицца дикшинари
        imgs_all = []
        labels_all = np.arange(0)
        features_all = np.arange(0)

        # loop all parts
        for part in self.parts:
            # get desease
            imgs, lbls = self.get_train_data_disease_parts(part)

            # create parts vector
            features = np.zeros(len(imgs))
            features.fill(self.parts[part])

            # add data
            imgs_all += imgs
            labels_all = np.append(labels_all, lbls)
            features_all = np.append(features_all, features)

        # возврат
        return imgs_all, labels_all, features_all


    # get data to train
    def __get_train_data_disease_category_part(self, part):
        # цикл по всем типам болезней - тока чтобы не выводить на верх класс ограничицца дикшинари
        imgs = []
        labels = np.arange(0)

        # loop disease types
        for dck, dcv in self.disease_tree.children.items():
            imgs_ds, lbls_ds = dcv.getImagesCats(dcv.code, part)
            imgs += imgs_ds
            labels = np.append(labels, lbls_ds)

        # возврат
        return imgs, labels


    # get data to train
    def get_train_data_disease_categories(self):
        # цикл по всем типам болезней - тока чтобы не выводить на верх класс ограничицца дикшинари
        imgs_all = []
        labels_all = np.arange(0)
        features_all = np.arange(0)

        # loop all parts
        for part in self.parts:
            #if part == 'leaf':
                # get desease
                imgs, lbls = self.__get_train_data_disease_category_part(part)

                # create parts vector
                features = np.zeros(len(imgs))
                features.fill(self.parts[part])

                # add data
                imgs_all += imgs
                labels_all = np.append(labels_all, lbls)
                features_all = np.append(features_all, features)

        # возврат
        return imgs_all, labels_all, features_all


    # train model for parts classification
    def trainmodel_parts(self, model_helper, epochs, tt_split, model_name):
        # get train data - loop all parts
        x_data, y_data = self.get_train_data_parts()

        # train model
        model = model_helper.train_model_generator(x_data, y_data, model_name, epochs, tt_split, self.get_name() +  " part")
        self.models['part'] = model
        return model


    # train model for diseases classification
    def trainmodel_disease_by_part(self, model_helper, part, epochs, tt_split, model_name):
        # get train data - loop all siseases
        x_data, y_data = self.get_train_data_disease_parts(part)

        # train model
        model = model_helper.train_model_generator(x_data, y_data, model_name, epochs, tt_split, self.get_name() +  " diseases by " + part)
        self.models[part] = model
        return model


    # train model for all diseases classification
    def trainmodel_diseases(self, model_helper, epochs, tt_split, model_name):
        # loop all loaded parts
        for part in self.parts:
            self.trainmodel_disease_by_part(model_helper, part, epochs, tt_split, model_name)


    # train model disease_category
    def trainmodel_disease_category(self, model_helper, epochs, tt_split, model_name, part_to_train = None):
        # loop all loaded parts
        for part in self.parts:
            if part_to_train is None or part_to_train == part:
                # get train data - loop all parts
                x_data, y_data = self.__get_train_data_disease_category_part(part)
                x_data = np.asarray(x_data)

                # train model
                model = model_helper.train_model_generator(x_data, y_data, model_name, epochs, tt_split, self.get_name() + " disease category " + part)
                self.models['disease_cat_4_' + part] = model


    # train model disease_category
    def trainmodel_disease_category_features(self, model_helper, epochs, tt_split, model_name):
        # получаем данные
        x_data, y_data, parts = self.get_train_data_disease_categories()

        # рисуем чтобы не ошибиться
        #img_id = rnd.randrange(len(x_data))
        #imghp.drow_image(x_data[img_id])
        #print("Категория болезни %i, часть %i" % (y_data[img_id], parts[img_id]))

        # обучаем и сохраняем
        model = model_helper.train_model_features_generator(x_data, y_data, parts, model_name, epochs, tt_split, self.get_name() + " disease ")
        self.models['disease_category'] = model


    # train model disease_category
    def trainmodel_disease_features(self, model_helper, epochs, tt_split, model_name):
        # получаем данные
        x_data, y_data, parts = self.get_train_data_diseases()

        # рисуем чтобы не ошибиться
        #img_id = rnd.randrange(len(x_data))
        #imghp.drow_image(x_data[img_id])
        #print("Категория болезни %i, часть %i" % (y_data[img_id], parts[img_id]))

        # обучаем и сохраняем
        model = model_helper.train_model_features_generator(x_data, y_data, parts, model_name, epochs, tt_split,
                                                             self.get_name() + " disease ")
        self.models['disease'] = model


    # export model to directory
    def export_models(self, model_helper, model_path):
        # loop all models
        for model in self.models:
            model_val = self.models[model]
            export_path = model_helper.create_model_path(model_path, self.get_name(), None, model)
            model_helper.export_model(model_val, model, export_path)


    def report_models_inputoutput(self, model_helper):
        # show plant name
        print("Входы выходы моделей для %s:" % self.get_name())

        # loop all models
        for model in self.models:
            model_val = self.models[model]
            model_helper.print_model_inputoutput(model, model_val)



